package com.soywiz.korio.vfs

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UniversalVfsTest {
	@Test
	fun testProperVfsIsResolved() {
		assertTrue("file:///path/to/my/file".uniVfs.vfs is LocalVfs)
		assertTrue("http://google.es/".uniVfs.vfs is UrlVfs)
		assertTrue("https://google.es/".uniVfs.vfs is UrlVfs)
	}

	@Test
	fun testProperPathIsResolved() {
		assertEquals("/path/to/my/file", "file:///path/to/my/file".uniVfs.absolutePath)
		assertEquals("http://google.es/", "http://google.es/".uniVfs.absolutePath)
		assertEquals("https://google.es/", "https://google.es/".uniVfs.absolutePath)
	}
}
