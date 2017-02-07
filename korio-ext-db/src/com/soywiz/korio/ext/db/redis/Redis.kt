package com.soywiz.korio.ext.db.redis

import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.stream.readBytes
import com.soywiz.korio.stream.readLine
import com.soywiz.korio.stream.writeBytes
import com.soywiz.korio.util.quote
import com.soywiz.korio.util.substr
import java.nio.charset.Charset

class Redis(val client: AsyncClient, val charset: Charset = Charsets.UTF_8) {
	//private val reader = AsyncBufferedInputStream(client)
	private val reader = client

	companion object {
		operator suspend fun invoke(host: String = "127.0.0.1", port: Int = 6379, charset: Charset = Charsets.UTF_8, password: String? = null): Redis {
			val client = Redis(AsyncClient(host, port), charset)
			if (password != null) client.auth(password)
			return client
		}
	}

	suspend fun close() = this.client.close()

	private val ioQueue = AsyncThread()

	suspend fun readValue(): Any? = ioQueue { _readValue() }

	suspend private fun _readValue(): Any? {
		val line = reader.readLine().trim()

		return when (line[0]) {
			'+' -> line.substr(1) // Status reply
			'-' -> throw ResponseException(line.substr(1)) // Error reply
			':' -> line.substr(1).toLong() // Integer reply
			'$' -> { // Bulk replies
				val BytesToRead = line.substr(1).toInt()
				if (BytesToRead == -1) {
					null
				} else {
					val Data = reader.readBytes(BytesToRead)
					reader.readBytes(2)
					Data.toString(charset)
				}
			}
			'*' -> { // Array reply
				val arraySize = line.substr(1).toLong()
				(0 until arraySize).map { _readValue() }
			}
			else -> throw ResponseException("Unknown param type '" + line[0] + "'")
		}
	}

	suspend fun command(vararg args: Any?): Any? {
		var cmd = "*${args.size}\r\n"
		for (arg in args) {
			val sarg = "$arg"
			// Length of the argument.
			val size = sarg.toByteArray(charset).size
			cmd += "\$$size\r\n"
			cmd += sarg + "\r\n"
		}

		ioQueue {
			client.writeBytes(cmd.toByteArray())
		}

		return readValue()
	}

	suspend fun commandArray(vararg args: Any?): List<String> = (command(*args) as List<String>?) ?: listOf()
	suspend fun commandString(vararg args: Any?): String? = command(*args)?.toString()
	suspend fun commandLong(vararg args: Any?): Long = command(*args)?.toString()?.toLong() ?: 0L
	suspend fun commandUnit(vararg args: Any?): Unit = run { command(*args) }

	class ResponseException(message: String) : Exception(message)
}

// @TODO: Missing commands

suspend fun Redis.append(key: String, value: String) = commandString("append", key.quote(), value.quote())
suspend fun Redis.auth(password: String) = commandString("auth", password.quote())
suspend fun Redis.bgrewriteaof() = commandString("bgrewriteaof")
suspend fun Redis.bgsave() = commandString("bgsave")
suspend fun Redis.bitcount(key: String) = commandString("bitcount", key)
suspend fun Redis.bitcount(key: String, start: Int, end: Int) = commandString("bitcount", key, "$start", "$end")


suspend fun Redis.set(key: String, value: String) = commandString("set", key, value)
suspend fun Redis.get(key: String) = commandString("get", key)
suspend fun Redis.del(vararg keys: String) = commandString("del", *keys)
suspend fun Redis.echo(msg: String) = commandString("echo", msg)


suspend fun Redis.hset(key: String, member: String, value: String): Long = commandLong("hset", key, member, value)

suspend fun Redis.hget(key: String, member: String): String? = commandString("hget", key, member)
suspend fun Redis.hincrby(key: String, member: String, increment: Long): Long = commandLong("hincrby", key, member, "$increment")
suspend fun Redis.zadd(key: String, vararg scores: Pair<String, Double>): Long {
	val args = arrayListOf<Any?>()
	for (score in scores) {
		args += score.second
		args += score.first.quote()
	}
	return commandLong("zadd", key, *args.toArray())
}

suspend fun Redis.zadd(key: String, member: String, score: Double): Long = commandLong("zadd", key, score, member.quote())
suspend fun Redis.sadd(key: String, member: String): Long = commandLong("sadd", key, member)
suspend fun Redis.smembers(key: String): List<String> = commandArray("smembers", key)

suspend fun Redis.zincrby(key: String, member: String, score: Double) = commandString("zincrby", key, score, member.quote())!!
suspend fun Redis.zcard(key: String): Long = commandLong("zcard", key)
suspend fun Redis.zrevrank(key: String, member: String): Long = commandLong("zrevrank", key, member.quote())
suspend fun Redis.zscore(key: String, member: String): Long = commandLong("zscore", key, member.quote())

private fun List<Any?>.listOfPairsToMap(): Map<String, String> {
	val list = this
	return (0 until list.size / 2).map { ("" + list[it * 2 + 0]) to ("" + list[it * 2 + 1]) }.toMap()
}

suspend fun Redis.hgetall(key: String): Map<String, String> {
	return commandArray("hgetall", key).listOfPairsToMap()
}

suspend fun Redis.zrevrange(key: String, start: Long, stop: Long): Map<String, Double> {
	return commandArray("zrevrange", key, start, stop, "WITHSCORES").listOfPairsToMap().mapValues { "${it.value}".toDouble() }
}

////