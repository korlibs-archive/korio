package com.soywiz.korio.vfs

import org.junit.Assert
import org.junit.Test

class PathInfoTest {
	@Test
	fun name() {
		PathInfo("/test/hello.TxT").apply {
			Assert.assertEquals("/test/hello.TxT", fullpath)
			Assert.assertEquals("/test/hello", pathWithoutExtension)
			Assert.assertEquals("/test/hello", fullnameWithoutCompoundExtension)
			Assert.assertEquals("/test", folder)
			Assert.assertEquals("hello.TxT", basename)
			Assert.assertEquals("hello", basenameWithoutExtension)
			Assert.assertEquals("TxT", extension)
			Assert.assertEquals("txt", extensionLC)
		}
	}

	@Test
	fun name2() {
		PathInfo("C:\\dev\\test\\hello.TxT").apply {
			Assert.assertEquals("C:\\dev\\test\\hello.TxT", fullpath)
			Assert.assertEquals("C:\\dev\\test\\hello", pathWithoutExtension)
			Assert.assertEquals("C:\\dev\\test\\hello", fullnameWithoutCompoundExtension)
			Assert.assertEquals("C:\\dev\\test", folder)
			Assert.assertEquals("hello.TxT", basename)
			Assert.assertEquals("hello", basenameWithoutExtension)
			Assert.assertEquals("TxT", extension)
			Assert.assertEquals("txt", extensionLC)
		}
	}

	@Test
	fun name3() {
		PathInfo("C:\\dev\\test\\hello").apply {
			Assert.assertEquals("C:\\dev\\test\\hello", fullpath)
			Assert.assertEquals("C:\\dev\\test\\hello", pathWithoutExtension)
			Assert.assertEquals("C:\\dev\\test\\hello", fullnameWithoutCompoundExtension)
			Assert.assertEquals("C:\\dev\\test", folder)
			Assert.assertEquals("hello", basename)
			Assert.assertEquals("hello", basenameWithoutExtension)
			Assert.assertEquals("", extension)
			Assert.assertEquals("", extensionLC)
		}
	}

	@Test
	fun name4() {
		PathInfo("C:\\dev\\test\\hello.Voice.Wav").apply {
			Assert.assertEquals("C:\\dev\\test\\hello.Voice.Wav", fullpath)
			Assert.assertEquals("C:\\dev\\test\\hello.Voice", fullnameWithoutExtension)
			Assert.assertEquals("C:\\dev\\test\\hello", pathWithoutExtension)
			Assert.assertEquals("C:\\dev\\test\\hello", fullnameWithoutCompoundExtension)
			Assert.assertEquals("C:\\dev\\test", folder)
			Assert.assertEquals("hello.Voice.Wav", basename)
			Assert.assertEquals("hello.Voice", basenameWithoutExtension)
			Assert.assertEquals("hello", basenameWithoutCompoundExtension)
			Assert.assertEquals("Wav", extension)
			Assert.assertEquals("wav", extensionLC)
			Assert.assertEquals("Voice.Wav", compoundExtension)
			Assert.assertEquals("voice.wav", compoundExtensionLC)
		}
	}

	@Test
	fun name5() {
		PathInfo("C:\\dev\\test.demo\\hello.Voice.Wav").apply {
			Assert.assertEquals("C:\\dev\\test.demo\\hello.Voice.Wav", fullpath)
			Assert.assertEquals("C:\\dev\\test.demo\\hello.Voice", fullnameWithoutExtension)
			Assert.assertEquals("C:\\dev\\test.demo\\hello", pathWithoutExtension)
			Assert.assertEquals("C:\\dev\\test.demo\\hello", fullnameWithoutCompoundExtension)
			Assert.assertEquals("C:\\dev\\test.demo", folder)
			Assert.assertEquals("hello.Voice.Wav", basename)
			Assert.assertEquals("hello.Voice", basenameWithoutExtension)
			Assert.assertEquals("hello", basenameWithoutCompoundExtension)
			Assert.assertEquals("Wav", extension)
			Assert.assertEquals("wav", extensionLC)
			Assert.assertEquals("Voice.Wav", compoundExtension)
			Assert.assertEquals("voice.wav", compoundExtensionLC)
		}
	}

	@Test
	fun getFullComponents() {
		Assert.assertEquals(listOf("a", "b", "c"), PathInfo("a/b/c").getComponents())
		Assert.assertEquals(listOf("a", "a/b", "a/b/c"), PathInfo("a/b/c").getFullComponents())
		Assert.assertEquals(listOf("a", "a/b", "a/b/"), PathInfo("a/b/").getFullComponents())
	}
}