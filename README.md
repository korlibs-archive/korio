# coktvfs

[![Build Status](https://travis-ci.org/soywiz/coktvfs.svg?branch=master)](https://travis-ci.org/soywiz/coktvfs)

[![Maven Version](https://img.shields.io/github/tag/soywiz/coktvfs.svg?style=flat&label=maven)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22coktvfs%22)

## Virtual File System for Kotlin Coroutines

Use with gradle:

```
compile "com.soywiz:coktvfs:0.1"
```

This is a Virtual File System that provides asynchronous I/O and filesystem operations for custom and extensible
filesystems with an homogeneous API. This repository doesn't require any special library dependency and just
requires Kotlin 1.1-M04 or greater.

As an example, in a suspend block, you can do the following:

```
val zip = ResourcesVfs()["hello.zip"].openAsZip()
for (file in zip.listRecursively()) {
    println(file.name)
}
```

There are several filesystems included:

```
LocalVfs, UrlVfs, ZipVfs, Resourcesvfs
```

But since it is extensible you can create custom ones (for S3, for Windows Registry, for FTP/SFTP, an ISO file...).

This is currently a working progress but already usable for some use-cases.