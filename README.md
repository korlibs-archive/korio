# korio

[![Build Status](https://travis-ci.org/soywiz/korio.svg?branch=master)](https://travis-ci.org/soywiz/korio)

[![Maven Version](https://img.shields.io/github/tag/soywiz/korio.svg?style=flat&label=maven)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22korio%22)

## Kotlin cORoutines I/O : Streams + Virtual File System

Use with gradle:

I'm uploading it to bintray and maven central:

For bintray:
```
maven { url "https://dl.bintray.com/soywiz/soywiz-maven" }
```

```
compile "com.soywiz:korio:$korioVersion"
```

This is a kotlin coroutine library that provides asynchronous nonblocking I/O and virtual filesystem operations
for custom and extensible filesystems with an homogeneous API. This repository doesn't require any special library
dependency and just requires Kotlin 1.1-M04 or greater.

This library is specially useful for webserver where asynchronous is the way to go. And completely asynchronous or
single threaded targets like javascript or as3, with kotlin-js or jtransc.

### Streams

Korio provides AsyncStream and SyncStream classes with a simplified readable, writable and seekable API,
for reading binary and text potentially huge data from files, network or whatever.
AsyncStream is designed to be able to read from disk or network asynchronously.
While SyncStream is designed to be able to read in-memory data faster while keeping the same API.

Both stream classes allow to read and write raw bytes, little and big endian primitive data, strings and structs while
allowing optimized stream slicing and reading for a simple binary file handling.

Some stream methods:
```
read, write
setPosition, getPosition
setLength, getLength
getAvailable, sliceWithSize, sliceWithBounds, slice, readSlice
readStringz, readString, readExact, readBytes, readBytesExact
readU8, readU16_le, readU32_le, readS16_le, readS32_le, readS64_le, readF32_le, readF64_le, readU16_be, readU32_be, readS16_be, readS32_be, readS64_be, readF32_be, readF64_be, readAvailable
writeBytes, write8, write16_le, write32_le, write64_le, writeF32_le, writeF64_le, write16_be, write32_be, write64_be, writeF32_be, writeF64_be
```

### VFS

Korio provides an asynchronous VirtualFileSystem extensible engine.
There is a Vfs class and a Vfs.Proxy class that provides you a base for your VFS. But at the application level, you
are using a VfsFile class that represents a file inside a Vfs.

As an example, in a suspend block, you can do the following:

```kotlin
val zip = ResourcesVfs()["hello.zip"].openAsZip() // Non blocking opening zip file
for (file in zip.listRecursively()) { // Lazy non blocking recursive file listing
    println(file.name)
}
```

### Jails

In order to increase security, Vfs engine provides a JailVfs that allows you to sandbox VFS operations inside an
specific folder. So you can do the following:

```kotlin
val base = LocalVfs(File("/path/to/sandbox/folder")).jail()
base["../../../etc/passwd"].readString() // this won't work
```

### Mounts

Korio includes a MountableVfs that allows you to mount other filesystems like this:

```
val resources = ResourcesVfs()
val root = MountableVfs({
	mount("/zip", resources["hello.zip"].openAsZip())
	mount("/iso", resources["isotest.iso"].openAsIso())
})
Assert.assertEquals("ZIP!", root["/zip/hello/world.txt"].readString())
Assert.assertEquals("ISO!", root["/iso/hello/world.txt"].readString())
```

### InMemory Vfs

Korio includes an inmemory vfs to create volatile vfs:

```
val mem = MemoryVfs(mapOf(
    "hello/secret.txt" to "SECRET!".toByteArray().openAsync(),
    "hello/world/test.txt" to "HELLO WORLD!".toByteArray().openAsync()
))
```

### NodeVfs

Korio includes an open base NodeVfs to support node based vfs.

### PathInfo

Korio includes a PathInfo utility integrated with VfsFile in order to obtain path information (folder, basename, extension...)

### Included Vfs

There are several filesystems included and you can find examples of usage in the test folder:

```kotlin
LocalVfs, UrlVfs, ZipVfs, IsoVfs, ResourcesVfs, JailVfs, MountableVfs, MemoryVfs
```

### API

To understand which kind of operations can be performed, this is the VfsFile API:

```kotlin
class VfsFile {
    val vfs: Vfs
    val path: String
    val basename: String
    operator fun get(path: String): VfsFile
    suspend fun open(mode: VfsOpenMode): AsyncStream
    suspend inline fun <reified T : Any> readSpecial(): T
    suspend fun read(): ByteArray
    suspend fun write(data: ByteArray): Unit
    suspend fun readString(charset: Charset = Charsets.UTF_8): String
    suspend fun writeString(data: String, charset: Charset = Charsets.UTF_8): Unit
    suspend fun readChunk(offset: Long, size: Int): ByteArray
    suspend fun writeChunk(data: ByteArray, offset: Long, resize: Boolean = false): Unit
    suspend fun stat(): VfsStat
    suspend fun size(): Long
    suspend fun exists(): Boolean
    suspend fun setSize(size: Long): Unit
    fun jail(): VfsFile = JailVfs(this)
    suspend fun list(): AsyncSequence<VfsStat>
    suspend fun listRecursive(): AsyncSequence<VfsStat>
}
```

You can create custom virtual file systems and combine them (for S3, for Windows Registry, for FTP/SFTP, an ISO file...)
or whatever you need.

Also, since you are using a single interface (VfsFile), you can create generic code that will work for files, for network,
for redis...
