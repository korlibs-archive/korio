package com.soywiz.korio.ext.db.mysql

import com.soywiz.korio.error.unsupported
import com.soywiz.korio.stream.*

class MysqlClient {
	companion object {
		const val DEFAULT_PORT = 3306;
	}

	enum class MyCharset(val id: Int) {
		unknown(-1),
		latin1_swedish_ci(8),
		utf8_general_ci(33),
		binary(63);

		companion object {
			val BY_ID = values().map { it.id to it }.toMap()
		}
	}

	data class HandshakeRequest(
			val version: String
	)

	data class HandshakeResponse(
			val version: String
	)

	suspend fun readHandshake(s: AsyncInputStream): HandshakeRequest {
		val size = s.readS32_le()
		val info = s.readBytesExact(size).openSync()
		val protocolVersion = info.readU8()
		when (protocolVersion) {
			10 -> {
				val version = info.readStringz()
				val connectionId = info.readS32_le()
				val authPluginPartData = info.readStringz(8)
				val filler_1 = info.readU8()
				val capability_flags_1 = info.readU16_le()
				val character_set = MyCharset.BY_ID[info.readU8()] ?: MyCharset.unknown
				val status_flags = info.readU16_le()
				val capability_flags_2 = info.readU16_le()
				val auth_plugin_data_len = info.readU8()
				val auth_plugin_data = info.readBytes(auth_plugin_data_len)
				val auth_plugin_name = info.readStringz()
				println(protocolVersion)
				println(version)
				println(connectionId)
				println(authPluginPartData)
				println(filler_1)
				println(capability_flags_1)
				println(character_set)
				println(status_flags)
				println(capability_flags_2)
				println(auth_plugin_data_len)
				println(auth_plugin_name)
				println(info.available)

				return HandshakeRequest(
						version = version
				)
			}
			else -> unsupported("Unsupported mysql protocol version. $protocolVersion")
		}

	}

	suspend fun writeHandshake(s: AsyncOutputStream) {

	}
}