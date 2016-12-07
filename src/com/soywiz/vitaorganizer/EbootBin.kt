package com.soywiz.vitaorganizer

import com.soywiz.util.Stream2
import com.soywiz.util.open2
import java.io.InputStream

object EbootBin {
	fun isSafe(s: ByteArray): Boolean = !hasExtendedPermissions(s.open2("r"))

	fun isSafe(s: Stream2): Boolean = !hasExtendedPermissions(s)

    fun hasExtendedPermissions(s: Stream2): Boolean {
        val s2 = s.slice()
        s2.position = 0x80
        val authid = s2.readS64_le()
        return when (authid) {
            0x2F00000000000001L, 0x2F00000000000003L -> true
            else -> false
        }
    }

	fun hasExtendedPermissions(s: InputStream): Boolean {
		try {
			val authid = ByteArray(1)
			s.skip(0x80)

			val ret = s.read(authid)
			if (ret == 1) {
				return authid[0].toInt() != 2
			}
			println("hasExtendedPermissions::read failed")
			return true
		}
		catch (e: Throwable) {
			println("hasExtendedPermissions, exception arised")
			e.printStackTrace()
			return true
		}
	}

	fun setSecureInplace(data: ByteArray, secure: Boolean = true): ByteArray {
		if (secure) {
			data[0x80] = (data[0x80].toInt() and 1.inv()).toByte() // Remove bit 0
			data[0x80] = (data[0x80].toInt() or 2).toByte() // Set bit 1?
		} else {
			data[0x80] = (data[0x80].toInt() or 1).toByte() // Set bit 0
		}
		return data
	}
}
