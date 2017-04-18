package com.soywiz.korio.vfs

data class AsyncFileLoader<T, TCtx>(val loader: suspend VfsFile.(TCtx) -> T)
