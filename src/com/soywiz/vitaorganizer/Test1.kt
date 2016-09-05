package com.soywiz.vitaorganizer

import java.util.*

object Test1 {
	@JvmStatic fun main(args: Array<String>) {
		PsvitaDevice.setIp("192.168.137.55", 1337)
		println(PsvitaDevice.getGameSize("PCSG00683"))
	}
}