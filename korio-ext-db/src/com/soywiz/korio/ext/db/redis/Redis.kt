package com.soywiz.korio.ext.db.redis

import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.stream.readBytes
import com.soywiz.korio.stream.readLine
import com.soywiz.korio.stream.writeBytes
import com.soywiz.korio.util.substr
import java.nio.charset.Charset

class Redis(val client: AsyncClient, val charset: Charset = Charsets.UTF_8) {
	companion object {
		operator suspend fun invoke(host: String = "127.0.0.1", port: Int = 6379, charset: Charset = Charsets.UTF_8, password: String? = null): Redis {
			val client = Redis(AsyncClient(host, port), charset)
			if (password != null) client.auth(password)
			return client
		}
	}

	suspend fun close() = this.client.close()

	private val readQueue = AsyncThread()

	suspend fun readValue(): Any? = readQueue {
		val FirstLine = client.readLine().trim()

		val v: Any? = when (FirstLine[0]) {
			'+' -> FirstLine.substr(1) // Status reply
			'-' -> throw ResponseException(FirstLine.substr(1)) // Error reply
			':' -> FirstLine.substr(1).toLong() // Integer reply
			'$' -> { // Bulk replies
				val BytesToRead = FirstLine.substr(1).toInt()
				if (BytesToRead == -1) {
					null
				} else {
					val Data = client.readBytes(BytesToRead)
					client.readBytes(2)
					Data.toString(charset)
				}
			}
			'*' -> { // Array reply
				val BulksToRead = FirstLine.substr(1).toLong()
				(0 until BulksToRead).map { readValue() }
			}
			else -> throw ResponseException("Unknown param type '" + FirstLine[0] + "'")
		}
		v
	}

	suspend fun command(vararg args: String): Any? {
		var Command = "*${args.size}\r\n"
		for (arg in args) {
			// Length of the argument.
			val size = arg.toByteArray(charset).size
			Command += "\$$size\r\n"
			Command += arg + "\r\n"
		}

		client.writeBytes(Command.toByteArray())

		return readValue()
	}

	class ResponseException(message: String) : Exception(message)
}

suspend fun Redis.append(key: String, value: String) = command("append", key, value) as String
suspend fun Redis.auth(password: String) = command("auth", password) as String
suspend fun Redis.bgrewriteaof() = command("bgrewriteaof") as String
suspend fun Redis.bgsave() = command("bgsave") as String
suspend fun Redis.bitcount(key: String) = command("bitcount", key) as String
suspend fun Redis.bitcount(key: String, start: Int, end: Int) = command("bitcount", key, "$start", "$end") as String


suspend fun Redis.set(key: String, value: String) = command("set", key, value) as String
suspend fun Redis.get(key: String) = command("get", key) as String?
suspend fun Redis.del(vararg keys: String) = command("del", *keys) as String?
suspend fun Redis.echo(msg: String) = command("echo", msg) as String?
