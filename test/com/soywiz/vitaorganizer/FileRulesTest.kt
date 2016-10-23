package com.soywiz.vitaorganizer

import org.junit.Assert
import org.junit.Test

class FileRulesTest {
	val TEST_FILES = listOf("eboot.bin", "other/inside/folder/file.dat", "sce_sys/clearsign", "data/mydata.bin", "sce_sys/keystone", "sce_sys/icon0.png", "sce_sys/param.sfo")

	@Test
	fun testIncludeInSmallVpk() {
		Assert.assertEquals(
			listOf("eboot.bin", "sce_sys/icon0.png", "sce_sys/param.sfo"),
			TEST_FILES.filter { FileRules.includeInSmallVpk(it) }
		)
	}

	@Test
	fun includeInBigVpk() {
		Assert.assertEquals(
			listOf("eboot.bin", "other/inside/folder/file.dat", "data/mydata.bin", "sce_sys/icon0.png", "sce_sys/param.sfo"),
			TEST_FILES.filter { FileRules.includeInBigVpk(it) }
		)
	}


	@Test
	fun includeInData() {
		Assert.assertEquals(
			listOf("other/inside/folder/file.dat", "data/mydata.bin"),
			TEST_FILES.filter { FileRules.includeInData(it) }
		)
	}
}