package com.soywiz.korio.ext.db.redis

import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.ds.AsyncPool
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.net.HostWithPort
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.AsyncCloseable
import com.soywiz.korio.util.substr
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicLong

// https://redis.io/topics/protocol
class Redis(val maxConnections: Int = 50, val stats: Stats = Stats(), private val clientFactory: suspend () -> Client) : RedisCommand {
	companion object {
		operator suspend fun invoke(hosts: List<String> = listOf("127.0.0.1:6379"), maxConnections: Int = 50, charset: Charset = Charsets.UTF_8, password: String? = null, stats: Stats = Stats()): Redis {
			val hostsWithPorts = hosts.map { HostWithPort.parse(it, 6379) }

			var index: Int = 0

			return Redis(maxConnections, stats) {
				val host = hostsWithPorts[index++ % hostsWithPorts.size] // Round Robin
				val tcpClient = AsyncClient(host.host, host.port)
				val client = Client(tcpClient, tcpClient, tcpClient, charset, stats)
				if (password != null) client.auth(password)
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

	class Client(reader: AsyncInputStream, val writer: AsyncOutputStream, val close: AsyncCloseable, val charset: Charset = Charsets.UTF_8, val stats: Stats = Stats()) : RedisCommand {
		private val reader = reader.toBuffered(bufferSize = 0x100)

		suspend fun close() = this.close.close()

		private val commandQueue = AsyncThread()

		suspend private fun readValue(): Any? {
			val line = reader.readBufferedUntil(LF).toString(charset).trim()
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
						val data = reader.readBytes(bytesToRead)
						reader.skip(2) // CR LF
						data.toString(charset)
					}
				}
				'*' -> { // Array reply
					val arraySize = line.substr(1).toLong()
					(0 until arraySize).map { readValue() }
				}
				else -> throw ResponseException("Unknown param type '" + line[0] + "'")
			}
		}

		suspend override fun commandAny(vararg args: Any?): Any? {
			stats.commandsQueued.incrementAndGet()
			return commandQueue {
				stats.commandsStarted.incrementAndGet()
				try {
					val cmd = StringBuilder()
					cmd.append('*')
					cmd.append(args.size)
					cmd.append("\r\n")
					for (arg in args) {
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
					val data = cmd.toString().toByteArray(charset)
					stats.commandsPreWritten.incrementAndGet()
					writer.writeBytes(data)
					stats.commandsWritten.incrementAndGet()
					val res = readValue()
					stats.commandsFinished.incrementAndGet()
					res
				} catch (t: Throwable) {
					stats.commandsErrored.incrementAndGet()
					println(t)
					throw t
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
suspend fun RedisCommand.commandLong(vararg args: Any?): Long = commandAny(*args)?.toString()?.toLong() ?: 0L
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
