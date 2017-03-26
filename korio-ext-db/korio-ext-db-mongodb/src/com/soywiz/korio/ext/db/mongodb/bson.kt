import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.mapWhile

object BSON {
	fun readDocument(s: SyncStream): Map<String, Any?> {
		val len = s.readS32_le()
		return s.readStream(len).readElementList().toMap()
	}

	fun writeDocument(doc: Map<String, Any?>, out: SyncStream) {
		for ((k, v) in doc) {

		}

		out.position = 0
		out.write32_le(out.length)
	}

	fun decode(data: ByteArray) = readDocument(data.openSync())
	fun encode(doc: Map<String, Any?>) = MemorySyncStreamToByteArray { writeDocument(doc, this) }

	fun SyncStream.readElementList() = mapWhile(cond = { !eof }, gen = { readElement() })

	fun SyncStream.readElementName() = readStringz()

	fun SyncStream.readStringWithLen(): String {
		val size = readS32_le()
		val data = readString(size)
		if (readU8() != 0) invalidOp("Expected zero")
		return data
	}

	fun SyncStream.readElement(): Pair<String, Any?> {
		val type = readU8()
		val name = readElementName()
		return when (type) {
			0x01 -> name to readF64_le()
			0x02 -> name to readStringWithLen()
			0x03 -> name to readDocument(this)
			0x04 -> name to readDocument(this).values.toList()
			0x05 -> name to readBinary()
			else -> TODO()
		}
	}

	fun SyncStream.readBinary(): ByteArray {
		val size = readS32_le()
		val subtype = readU8()
		return readBytes(size)
	}
}