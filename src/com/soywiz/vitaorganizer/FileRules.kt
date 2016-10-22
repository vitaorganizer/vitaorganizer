package com.soywiz.vitaorganizer

object FileRules {
	fun isEboot(path: String) = path == "eboot.bin"
	fun isSceSysFolder(path: String) = path.startsWith("sce_sys/")

	// Skip these files since it seems that having them prevents the vpk from installing (this should allow to install dumps with maidumptool)
	fun isClearsignOrKeystone(path: String) = (path == "sce_sys/clearsign") || (path == "sce_sys/keystone")

	fun includeInSmallVpk(path: String) = isEboot(path) || (isSceSysFolder(path) && !isClearsignOrKeystone(path))

	fun includeInBigVpk(path: String) = !isClearsignOrKeystone(path)

	// Skip files already installed in the VPK
	fun includeInData(path: String) = !isEboot(path) && !isSceSysFolder(path)
}