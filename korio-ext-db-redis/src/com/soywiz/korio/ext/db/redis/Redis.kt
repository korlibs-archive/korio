package com.soywiz.korio.ext.db.redis

import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.AsyncCloseable
import com.soywiz.korio.util.substr
import java.nio.charset.Charset

// https://redis.io/topics/protocol
class Redis(reader: AsyncInputStream, val writer: AsyncOutputStream, val close: AsyncCloseable, val charset: Charset = Charsets.UTF_8) {
	private val reader = AsyncBufferedInputStream(reader, bufferSize = 0x100)
	//private val reader = reader

	companion object {
		operator suspend fun invoke(host: String = "127.0.0.1", port: Int = 6379, charset: Charset = Charsets.UTF_8, password: String? = null): Redis {
			val tcpClient = AsyncClient(host, port)
			val client = Redis(tcpClient, tcpClient, tcpClient, charset)
			if (password != null) client.auth(password)
			return client
		}

		private const val CR = '\r'.toByte()
		private const val LF = '\n'.toByte()
	}

	suspend fun close() = this.close.close()

	private val ioQueue = AsyncThread()

	suspend private fun readValue(): Any? = ioQueue { _readValue() }

	suspend private fun _readValue(): Any? {
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
				(0 until arraySize).map { _readValue() }
			}
			else -> throw ResponseException("Unknown param type '" + line[0] + "'")
		}
	}

	suspend fun commandAny(vararg args: Any?): Any? {
		var cmd = "*${args.size}\r\n"
		for (arg in args) {
			val sarg = "$arg"
			// Length of the argument.
			val size = sarg.toByteArray(charset).size
			cmd += '$'
			cmd += Integer.toString(size)
			cmd += "\r\n"
			cmd += sarg
			cmd += "\r\n"
		}

		// Queue just required for reading since Redis support pipelining : https://redis.io/topics/pipelining
		//ioQueue { }
		writer.writeBytes(cmd.toByteArray(charset))

		return readValue()
	}

	suspend fun commandArray(vararg args: Any?): List<String> = (commandAny(*args) as List<String>?) ?: listOf()
	suspend fun commandString(vararg args: Any?): String? = commandAny(*args)?.toString()
	suspend fun commandLong(vararg args: Any?): Long = commandAny(*args)?.toString()?.toLong() ?: 0L
	suspend fun commandUnit(vararg args: Any?): Unit = run { commandAny(*args) }

	class ResponseException(message: String) : Exception(message)
}

