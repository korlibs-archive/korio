package com.soywiz.korio.ext.db.redis

import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.async.sleep
import com.soywiz.korio.coroutine.withCoroutineContext
import com.soywiz.korio.ds.AsyncPool
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.net.HostWithPort
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.AsyncCloseable
import com.soywiz.korio.util.Once
import com.soywiz.korio.util.substr

// https://redis.io/topics/protocol
class Redis(val maxConnections: Int = 50, val stats: Stats = Stats(), private val clientFactory: suspend () -> Client) : RedisCommand {
	companion object {
		operator suspend fun invoke(hosts: List<String> = listOf("127.0.0.1:6379"), maxConnections: Int = 50, charset: Charset = Charsets.UTF_8, password: String? = null, stats: Stats = Stats(), bufferSize: Int = 0x1000): Redis {
			val hostsWithPorts = hosts.map { HostWithPort.parse(it, 6379) }

			var index: Int = 0

			return Redis(maxConnections, stats) {
				val tcpClient = AsyncClient.create()
				val client = Client(
					reader = tcpClient,
					reconnect = { client ->
						index = (index + 1) % hostsWithPorts.size
						val host = hostsWithPorts[index] // Round Robin
						tcpClient.connect(host.host, host.port)
						if (password != null) client.auth(password)
					},
					writer = tcpClient,
					closeable = tcpClient,
					charset = charset,
					stats = stats,
					bufferSize = bufferSize
				)
				client
			}
		}

		private const val CR = '\r'.toByte()
		private const val LF = '\n'.toByte()
	}

	class Stats {
		val commandsQueued = AtomicLong()
		val commandsStarted = AtomicLong()
		val commandsPreWritten = AtomicLong()
		val commandsWritten = AtomicLong()
		val commandsErrored = AtomicLong()
		val commandsFinished = AtomicLong()

		override fun toString(): String {
			return "Stats(commandsQueued=$commandsQueued, commandsStarted=$commandsStarted, commandsPreWritten=$commandsPreWritten, commandsWritten=$commandsWritten, commandsErrored=$commandsErrored, commandsFinished=$commandsFinished)"
		}
	}

	class Client(
		reader: AsyncInputStream,
		val writer: AsyncOutputStream,
		val closeable: AsyncCloseable,
		val charset: Charset = Charsets.UTF_8,
		val stats: Stats = Stats(),
		val bufferSize: Int = 0x1000,
		val reconnect: suspend (Client) -> Unit = {}
	) : RedisCommand {
		private val reader = reader.toBuffered(bufferSize = bufferSize)

		suspend fun close() = this.closeable.close()

		private val once = Once()
		private val commandQueue = AsyncThread()

		suspend private fun initOnce() {
			once {
				commandQueue.sync {
					try {
						reconnect(this@Client)
					} catch (e: IOException) {
					}
				}
			}
		}

		companion object {
			//const val DEBUG = true
			const val DEBUG = false
		}

		suspend private fun readValue(): Any? {
			val line = reader.readBufferedUntil(LF).toString(charset).trim()
			//val line = reader.readUntil(LF).toString(charset).trim()
			if (DEBUG) println("Redis[RECV]: $line")
			//val line = reader.readLine(charset = charset).trim()
			//println(line)

			return when (line[0]) {
				'+' -> line.substr(1) // Status reply
				'-' -> throw ResponseException(line.substr(1)) // Error reply
				':' -> line.substr(1).toLong() // Integer reply
				'$' -> { // Bulk replies
					val bytesToRead = line.substr(1).toInt()
					if (bytesToRead == -1) {
						null
					} else {
						val data = reader.readBytesExact(bytesToRead)
						reader.skip(2) // CR LF
						val out = data.toString(charset)
						if (DEBUG) println("Redis[RECV][data]: $out")
						out
					}
				}
				'*' -> { // Array reply
					val arraySize = line.substr(1).toLong()
					(0 until arraySize).map { readValue() }
				}
				else -> throw ResponseException("Unknown param type '" + line[0] + "'")
			}
		}

		val maxRetries = 10

		suspend override fun commandAny(vararg args: Any?): Any? = withCoroutineContext {
			//println(args.toList())
			stats.commandsQueued.incrementAndGet()
			return@withCoroutineContext commandQueue {
				val cmd = StringBuilder()
				cmd.append('*')
				cmd.append(args.size)
				cmd.append("\r\n")
				for (arg in args) {
					//val sarg = "$arg".redisQuoteIfRequired()
					val sarg = "$arg"
					// Length of the argument.
					val size = sarg.toByteArray(charset).size
					cmd.append('$')
					cmd.append(size)
					cmd.append("\r\n")
					cmd.append(sarg)
					cmd.append("\r\n")
				}

				// Common queue is not required align reading because Redis support pipelining : https://redis.io/topics/pipelining
				val dataString = cmd.toString()
				val data = dataString.toByteArray(charset)
				var retryCount = 0

				if (DEBUG) println("Redis[SEND]: $dataString")

				retry@ while (true) {
					stats.commandsStarted.incrementAndGet()
					try {
						stats.commandsPreWritten.incrementAndGet()
						writer.writeBytes(data)
						stats.commandsWritten.incrementAndGet()
						val res = readValue()
						stats.commandsFinished.incrementAndGet()
						return@commandQueue res
					} catch (t: IOException) {
						stats.commandsErrored.incrementAndGet()
						try {
							reconnect(this@Client)
						} catch (e: Throwable) {
						}
						sleep(500 * retryCount)
						retryCount++
						if (retryCount < maxRetries) {
							continue@retry
						} else {
							throw RuntimeException("Giving Up with this redis request max retries $maxRetries")
						}
					} catch (t: Throwable) {
						stats.commandsErrored.incrementAndGet()
						println(t)
						throw t
					}
				}
			}
		}
	}

	private val clientPool = AsyncPool(maxItems = maxConnections) { clientFactory() }

	suspend override fun commandAny(vararg args: Any?): Any? = clientPool.tempAlloc { it.commandAny(*args) }

	class ResponseException(message: String) : Exception(message)
}

interface RedisCommand {
	suspend fun commandAny(vararg args: Any?): Any?
}

@Suppress("UNCHECKED_CAST")
suspend fun RedisCommand.commandArray(vararg args: Any?): List<String> = (commandAny(*args) as List<String>?) ?: listOf()

suspend fun RedisCommand.commandString(vararg args: Any?): String? = commandAny(*args)?.toString()
suspend fun RedisCommand.commandLong(vararg args: Any?): Long = commandAny(*args)?.toString()?.toLongOrNull() ?: 0L
suspend fun RedisCommand.commandUnit(vararg args: Any?): Unit = run { commandAny(*args) }

// @TODO: SLOWER:
//val cmd = ByteArrayOutputStream()
//val ps = PrintStream(cmd, true, Charsets.UTF_8.name())
//
//ps.print('*')
//ps.print(args.size)
//ps.print("\r\n")
//for (arg in args) {
//	val data = "$arg".toByteArray(charset)
//	ps.print('$')
//	ps.print(data.size)
//	ps.print("\r\n")
//	ps.write(data)
//	ps.print("\r\n")
//}
//
//// Common queue is not required align reading because Redis support pipelining : https://redis.io/topics/pipelining
//return commandQueue {
//	writer.writeBytes(cmd.toByteArray())
//	readValue()
//}
