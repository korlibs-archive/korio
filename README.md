## Korio: Kotlin cORoutines I/O : Streams + Async TCP Client/Server + Virtual File System for JVM, Kotlin-JS, Android, Jtransc+Node.JS and Jtransc+Browser

[![Build Status](https://travis-ci.org/soywiz/korio.svg?branch=master)](https://travis-ci.org/soywiz/korio)
[![Maven Version](https://img.shields.io/github/tag/soywiz/korio.svg?style=flat&label=maven)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22korio%22)

![](https://raw.githubusercontent.com/soywiz/kor/master/logos/128/korio.png)

[All KOR libraries](https://github.com/soywiz/kor)

Use with gradle (uploaded to maven central):

```
compile "com.soywiz:korio:$korVersion"
```

I'm also uploading it to a personal bintray repository:

```
maven { url "https://dl.bintray.com/soywiz/soywiz-maven" }
```

This is a kotlin coroutine library that provides asynchronous non-blocking I/O and virtual filesystem operations
for custom and extensible filesystems with an homogeneous API. This repository doesn't require any special library
dependency and just requires Kotlin 1.1-M04 or greater.

This library is specially useful for webserver where asynchronous is the way to go. And completely asynchronous or
single threaded targets like JavaScript or AS3, with kotlin-js or JTransc (Node.JS and Browser). So if you use
korio you will be able to target several platforms without any problem.

It has a modern and useful API. And all works in Java 7, so it is compatible with Android.

### Event Loop and async primitives

Korio provides an Event Loop that integrates with each supported platform seamlessly.
So in JS the event loop will use setTimeout and setInterval, and will queue actions with it.
In the case of Android it will use runOnUiThread and timer primitives,
and in the JVM it would use en emulated Event Loop.
You can even create your own event loop implementation and hook it.

Korio also provides some async primitives until they are officially available
at a common place like kotlinx.coroutines, and will provide typealias + @Deprecated for the future migration
when available.

### Serialization

Embeded **Json**, **Xml** and **Yaml** parsers. Can also write Json (with pretty print support) and Xml. Support Json to Object mapping with kotlin data classes suport.

### Streams

Korio provides AsyncStream and SyncStream classes with a simplified readable, writable and seekable API,
for reading binary and text potentially huge data from files, network or whatever.
AsyncStream is designed to be able to read from disk or network asynchronously.
While SyncStream is designed to be able to read in-memory data faster while keeping the same API.

Both stream classes allow to read and write raw bytes, little and big endian primitive data, strings and structs while
allowing optimized stream slicing and reading for a simple binary file handling.

Some stream methods:
```kotlin
read, write
setPosition, getPosition
setLength, getLength
getAvailable, sliceWithSize, sliceWithBounds, slice, readSlice
readStringz, readString, readExact, readBytes, readBytesExact
readU8, readU16_le, readU32_le, readS16_le, readS32_le, readS64_le, readF32_le, readF64_le, readU16_be, readU32_be, readS16_be, readS32_be, readS64_be, readF32_be, readF64_be, readAvailable
writeBytes, write8, write16_le, write32_le, write64_le, writeF32_le, writeF64_le, write16_be, write32_be, write64_be, writeF32_be, writeF64_be
```

### AsyncClient + AsyncServer

Korio includes a TCP client (implementing AsyncStream) and a TCP server with a lazy asynchronous connection iterator for all supported platforms but browser javascript. 

### WebSocketClient

Korio includes a WebSocket client. It has two implementations: one simple and generic for targets supporting AsyncClient and other for browser javascript.
So this is supported on all targets.

### HttpClient

Korio includes a HttpClient client that uses available native implementations. UrlVfs uses HttpClient.

### Databases

#### Redis

Korio includes an extension with some database/cache clients. At this point, there is a Redis client implementation,
but will provide more soon.

#### DynamoDB

Korio includes a pure Kotlin DynamoDB asynchronous simple and fast implementation using Korio's HttpClient implementations, which leverages:
jvm, android, js (browser and nodejs) + pure asynchronous http clients from vertx when including korio-vertx. 
 
### VFS

Korio provides an asynchronous Virtual File System extensible engine.
There is a Vfs class and a Vfs.Proxy class that provides you a base for your VFS.
But when using it you are using a VfsFile class that represents a node (file or folder) inside a Vfs.

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
val jail = LocalVfs(File("/path/to/sandbox/folder")).jail()
jail["../../../etc/passwd"].readString() // this won't work
```

### Mounts

Korio includes a MountableVfs that allows you to mount other filesystems like this:

```kotlin
val resources = ResourcesVfs
val root = MountableVfs {
	mount("/zip", resources["hello.zip"].openAsZip())
	mount("/iso", resources["isotest.iso"].openAsIso())
}
Assert.assertEquals("ZIP!", root["/zip/hello/world.txt"].readString())
Assert.assertEquals("ISO!", root["/iso/hello/world.txt"].readString())

(root.vfs as Mountable).unmount("/zip")
```

### Memory Vfs

Korio includes an inmemory vfs to create volatile vfs:

```kotlin
val mem = MemoryVfs(mapOf(
    "hello/secret.txt" to "SECRET!".toByteArray().openAsync(),
    "hello/world/test.txt" to "HELLO WORLD!".toByteArray().openAsync()
))
```

### NodeVfs

Korio includes an open base NodeVfs to support node based vfs like in-memory vfs.

### S3Vfs (Amazon S3 Vfs)

In the korio-ext-amazon-s3 submodule, there is a Amazon S3 Client without external dependencies that is implemented as a VFS for convenience. 

### PathInfo

Korio includes a PathInfo utility integrated with VfsFile in order to obtain path information (folder, basename, extension...)

### Execution

Korio includes includes a mechanism to execute commands inside a VfsFile. This allow you execute asynchronously commands
in your local file system, but also allows to implement RPC mechanisms, that will work seamlessly. You can implement here
ssh or ftp commands as an example.

### Included Vfs

There are several filesystems included and you can find examples of usage in the test folder:

```kotlin
LocalVfs, UrlVfs, ZipVfs, IsoVfs, ResourcesVfs, JailVfs, MountableVfs, MemoryVfs, S3Vfs
```

For Vfs implementations:
```kotlin
Vfs, Vfs.Proxy, Vfs.Decorate, NodeVfs, MergedVfs
```

### API

To understand which kind of operations can be performed, this is the VfsFile API:

```kotlin
class VfsFile {
    val vfs: Vfs
    val path: String
    val basename: String
    val pathInfo: PathInfo

    // Accessing parent
    val parent: VfsFile

    // Accessing descendants, and ascendants using relative paths
    operator fun get(path: String): VfsFile

    // Opening file as AsyncStream
    suspend fun open(mode: VfsOpenMode): AsyncStream

    // Convenience methods for fully reading/writing files
    suspend fun read(): ByteArray
    suspend fun readString(charset: Charset = Charsets.UTF_8): String
    suspend fun readChunk(offset: Long, size: Int): ByteArray
	suspend fun readAsSyncStream(): SyncStream
    suspend fun write(data: ByteArray): Unit
    suspend fun writeString(data: String, charset: Charset = Charsets.UTF_8): Unit
    suspend fun writeChunk(data: ByteArray, offset: Long, resize: Boolean = false): Unit

    // Stat + convenience methods
    suspend fun stat(): VfsStat
    suspend fun size(): Long
    suspend fun exists(): Boolean

    // Modification operations
    suspend fun delete(): Boolean
    suspend fun mkdir(): Boolean
    suspend fun mkdirs(): Boolean
    suspend fun renameTo(path: String): Boolean
    suspend fun setSize(size: Long): Unit

    // Directory listing
    suspend fun list(): AsyncSequence<VfsFile>
    suspend fun listRecursive(): AsyncSequence<VfsFile>

    // Executing in this folder/filesystem. You can implement RPC here or ftp/sftp commands. Implemented in default korio for LocalVfs.
	suspend fun exec(cmdAndArgs: List<String>, handler: VfsProcessHandler = VfsProcessHandler()): Int

	// Convenience execution methods
	suspend fun execToString(cmdAndArgs: List<String>, charset: Charset = Charsets.UTF_8): String
	suspend fun execToString(vararg cmdAndArgs: String, charset: Charset = Charsets.UTF_8): String
	suspend fun passthru(cmdAndArgs: List<String>, charset: Charset = Charsets.UTF_8): Int
	suspend fun passthru(vararg cmdAndArgs: String, charset: Charset = Charsets.UTF_8): Int
	
	// File watching
	suspend fun watch(handler: (VfsFileEvent) -> Unit): Closeable = vfs.watch(path, handler)

    // Jail this file so generated VfsFile can't access ancestors
    fun jail(): VfsFile = JailVfs(this)
}

data class VfsStat {
	val file: VfsFile
	val exists: Boolean
	val isDirectory: Boolean
	val size: Long
	val device: Long
	val inode: Long
	val mode: Int
	val owner: String
	val group: String
	val createTime: Long
	val modifiedTime: Long
	val lastAccessTime: Long
	val extraInfo: Any?
}

class PathInfo(val fullpath: String) {
	val folder: String
	val basename: String
	val pathWithoutExtension: String
	val basenameWithoutExtension: String
	val extension: String
}
```

You can create custom virtual file systems and combine them (for S3, for Windows Registry, for FTP/SFTP, an ISO file...)
or whatever you need.

Also, since you are using a single interface (VfsFile), you can create generic code that will work for files, for network,
for redis...
You can use a MemoryVfs for testing while using a real folder in your code without having to mock code.

### Targets

Korio supports JVM, Android, Browser and Node.JS out of the box at this point. But it is extensible so you can create
your own targets or benefit from new ones when available.

Features:

* JVM uses NIO and common runtime tools
* Android uses threads when required and simplifies and unifies resources/assets loading/listing
* Node.JS uses the all asynchronous methods available and supports full korio
* Browser allows reading "embedded" resource lists supported by jtransc + reading urls chunks/streaming with buffering in same domain or with CORs. Do not support raw client/server sockets.

