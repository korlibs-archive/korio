# korio

[![Build Status](https://travis-ci.org/soywiz/korio.svg?branch=master)](https://travis-ci.org/soywiz/korio)

[![Maven Version](https://img.shields.io/github/tag/soywiz/korio.svg?style=flat&label=maven)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22korio%22)

## Kotlin cORoutines I/O : Streams + Virtual File System Edit

Use with gradle:

```
compile "com.soywiz:korio:0.1.1"
```

This is a kotlin coroutine library that provides asynchronous I/O and filesystem operations for custom and extensible
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
LocalVfs, UrlVfs, ZipVfs, IsoVfs, Resourcesvfs
```

But since it is extensible you can create custom ones (for S3, for Windows Registry, for FTP/SFTP, an ISO file...).

This is currently a working progress but already usable for some use-cases.