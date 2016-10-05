package com.soywiz.util

enum class DumperNames(val shortName: String, val longName: String, val file: String, val size: Long) {

    VTLEAK("VT_Leak", "Vitamin Leaked Version", DumperModules.VITAMIN.file, 110535L),
    VT1("VT1.0", "Vitamin 1.0 or 1.1", DumperModules.VITAMIN.file, 107851L),
    VT2("VT2.0", "Vitamin 2.0", DumperModules.VITAMIN.file, 78682L),
    MAI233("Mai.v233.0", "Mai.v233.0", DumperModules.MAI.file, 86442L),
    HOMEBREW("HB", "Normal homebrew", "", -1L),
    UNKNOWN("UNKNOWN", "Unknown Dumper Version", "", -1L)
}

enum class DumperModules(val file: String) {
    VITAMIN("sce_module/steroid.suprx"),
    MAI("mai_moe/mai.suprx")
}

class DumperNamesHelper {
    fun findDumperBySize(size: Long) : DumperNames {
        for (dumper in DumperNames.values()) {
            if (size == dumper.size)
                return dumper
        }
        return DumperNames.UNKNOWN
    }

    fun findDumperByShortName(shortName: String) : DumperNames {
        for (dumper in DumperNames.values()) {
            if (dumper.shortName.equals( shortName ))
                return dumper
        }
        return DumperNames.UNKNOWN
    }
}