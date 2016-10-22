package com.soywiz.util

import java.security.MessageDigest

object Hash {
	fun md5(data: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(data)
	fun sha1(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA1").digest(data)
}