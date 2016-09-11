package com.soywiz.vitaorganizer.i18n

import com.soywiz.vitaorganizer.TextFormatter
import org.junit.Assert
import org.junit.Test

class TextFormatterTest {
	@Test
	fun testFormat() {
		Assert.assertEquals(
			"hello 1 %a% hello",
			TextFormatter.format("hello %test% %a% %b%", mapOf("test" to 1, "b" to "hello"))
		)
	}
}