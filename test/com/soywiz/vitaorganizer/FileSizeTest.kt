package com.soywiz.vitaorganizer

import com.soywiz.vitaorganizer.ext.setTemporarily
import org.junit.Assert
import org.junit.Test
import java.util.*

class FileSizeTest {
	@Test
	fun testDefaultFileSize() {
		Assert.assertEquals("0 B", FileSize.toString(0L))
		Assert.assertEquals("1 KB", FileSize.toString(1024L))
		Assert.assertEquals("121 KB", FileSize.toString(123456L))
		Assert.assertEquals("11.8 MB", FileSize.toString(12345678L))
		Assert.assertEquals("11.498 GB", FileSize.toString(12345678901L))
		Assert.assertEquals("1.122833 TB", FileSize.toString(1234567890123L))
	}

	@Test fun testDefaultFileSizeEnglishLocale() = Locale.ENGLISH.setTemporarily { testDefaultFileSize() }
	@Test fun testDefaultFileSizeSpanishLocale() = Locale.forLanguageTag("es").setTemporarily { testDefaultFileSize() }
}