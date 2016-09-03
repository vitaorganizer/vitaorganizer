package com.soywiz.util

class InvalidOperationException(msg: String) : RuntimeException(msg)

fun noImpl(msg: String): Nothing = throw NotImplementedError(msg)
fun invalidOp(msg: String): Nothing = throw InvalidOperationException(msg)
val noImpl: Nothing get() = throw NotImplementedError()
val notMigrated: Nothing get() = throw NotImplementedError()