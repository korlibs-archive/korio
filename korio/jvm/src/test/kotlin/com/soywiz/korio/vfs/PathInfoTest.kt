package com.soywiz.korio.vfs

import org.junit.Test
import kotlin.test.assertEquals

class PathInfoTest {
	@Test
	fun name() {
		PathInfo("/test/hello.TxT").apply {
			assertEquals("/test/hello.TxT", fullpath)
			assertEquals("/test/hello", pathWithoutExtension)
			assertEquals("/test/hello", fullnameWithoutCompoundExtension)
			assertEquals("/test", folder)
			assertEquals("hello.TxT", basename)
			assertEquals("hello", basenameWithoutExtension)
			assertEquals("TxT", extension)
			assertEquals("txt", extensionLC)
		}
	}

	@Test
	fun name2() {
		PathInfo("C:\\dev\\test\\hello.TxT").apply {
			assertEquals("C:\\dev\\test\\hello.TxT", fullpath)
			assertEquals("C:\\dev\\test\\hello", pathWithoutExtension)
			assertEquals("C:\\dev\\test\\hello", fullnameWithoutCompoundExtension)
			assertEquals("C:\\dev\\test", folder)
			assertEquals("hello.TxT", basename)
			assertEquals("hello", basenameWithoutExtension)
			assertEquals("TxT", extension)
			assertEquals("txt", extensionLC)
		}
	}

	@Test
	fun name3() {
		PathInfo("C:\\dev\\test\\hello").apply {
			assertEquals("C:\\dev\\test\\hello", fullpath)
			assertEquals("C:\\dev\\test\\hello", pathWithoutExtension)
			assertEquals("C:\\dev\\test\\hello", fullnameWithoutCompoundExtension)
			assertEquals("C:\\dev\\test", folder)
			assertEquals("hello", basename)
			assertEquals("hello", basenameWithoutExtension)
			assertEquals("", extension)
			assertEquals("", extensionLC)
		}
	}

	@Test
	fun name4() {
		PathInfo("C:\\dev\\test\\hello.Voice.Wav").apply {
			assertEquals("C:\\dev\\test\\hello.Voice.Wav", fullpath)
			assertEquals("C:\\dev\\test\\hello.Voice", fullnameWithoutExtension)
			assertEquals("C:\\dev\\test\\hello", pathWithoutExtension)
			assertEquals("C:\\dev\\test\\hello", fullnameWithoutCompoundExtension)
			assertEquals("C:\\dev\\test", folder)
			assertEquals("hello.Voice.Wav", basename)
			assertEquals("hello.Voice", basenameWithoutExtension)
			assertEquals("hello", basenameWithoutCompoundExtension)
			assertEquals("Wav", extension)
			assertEquals("wav", extensionLC)
			assertEquals("Voice.Wav", compoundExtension)
			assertEquals("voice.wav", compoundExtensionLC)
		}
	}

	@Test
	fun name5() {
		PathInfo("C:\\dev\\test.demo\\hello.Voice.Wav").apply {
			assertEquals("C:\\dev\\test.demo\\hello.Voice.Wav", fullpath)
			assertEquals("C:\\dev\\test.demo\\hello.Voice", fullnameWithoutExtension)
			assertEquals("C:\\dev\\test.demo\\hello", pathWithoutExtension)
			assertEquals("C:\\dev\\test.demo\\hello", fullnameWithoutCompoundExtension)
			assertEquals("C:\\dev\\test.demo", folder)
			assertEquals("hello.Voice.Wav", basename)
			assertEquals("hello.Voice", basenameWithoutExtension)
			assertEquals("hello", basenameWithoutCompoundExtension)
			assertEquals("Wav", extension)
			assertEquals("wav", extensionLC)
			assertEquals("Voice.Wav", compoundExtension)
			assertEquals("voice.wav", compoundExtensionLC)
		}
	}

	@Test
	fun getFullComponents() {
		assertEquals(listOf("a", "b", "c"), PathInfo("a/b/c").getComponents())
		assertEquals(listOf("a", "a/b", "a/b/c"), PathInfo("a/b/c").getFullComponents())
		assertEquals(listOf("a", "a/b", "a/b/"), PathInfo("a/b/").getFullComponents())
	}
}