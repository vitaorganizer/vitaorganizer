package com.soywiz.vitaorganizer

import com.soywiz.vitaorganizer.ext.getResourceBytes
import com.soywiz.vitaorganizer.ext.getResourceStream2
import org.junit.Assert
import org.junit.Test

class EbootBinTest {
	@Test
	fun testSafe() {
		Assert.assertEquals(true, EbootBin.isSafe(getResourceStream2("helloworld.safe.eboot.bin")!!))
		Assert.assertEquals(false, EbootBin.isSafe(getResourceStream2("helloworld.unsafe.eboot.bin")!!))
	}

	@Test
	fun testMakeSafe() {
		Assert.assertEquals(true, EbootBin.isSafe(EbootBin.setSecureInplace(getResourceBytes("helloworld.safe.eboot.bin")!!)))
		Assert.assertEquals(true, EbootBin.isSafe(EbootBin.setSecureInplace(getResourceBytes("helloworld.unsafe.eboot.bin")!!)))
	}

	@Test
	fun testMakeUnsafe() {
		Assert.assertEquals(false, EbootBin.isSafe(EbootBin.setSecureInplace(getResourceBytes("helloworld.safe.eboot.bin")!!, secure = false)))
		Assert.assertEquals(false, EbootBin.isSafe(EbootBin.setSecureInplace(getResourceBytes("helloworld.unsafe.eboot.bin")!!, secure = false)))
	}
}