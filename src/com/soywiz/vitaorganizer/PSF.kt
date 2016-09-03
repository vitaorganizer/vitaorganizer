package com.soywiz.vitaorganizer

import com.soywiz.util.Stream2
import com.soywiz.util.byte
import com.soywiz.util.invalidOp

// http://www.psdevwiki.com/ps3/PARAM.SFO#TITLE_ID
// http://www.vitadevwiki.com/index.php?title=Title_ID
// http://www.psdevwiki.com/ps4/Productcode
// http://www.vitadevwiki.com/index.php?title=System_File_Object_(SFO)_(PSF)#TITLE_ID
object PSF {
    fun read(s: Stream2): Map<String, Any> {
        class Entry(
                val keyOffset: Int,
                val unknown: Int,
                val dataType: Int,
                val valueSize: Int,
                val valueSizePad: Int,
                val valueOffset: Int
        )

        val magic = s.readBytes(4)
        if (magic.toList() != byteArrayOf(0, 'P'.byte, 'S'.byte, 'F'.byte).toList()) {
            invalidOp("Not a PSF file : But: ${magic.toList()}")
        }

        val version = s.readS32_le()
        val keyTable = s.readS32_le()
        val valueTable = s.readS32_le()
        val numberOfPairs = s.readS32_le()

        val entries = (0 until numberOfPairs).map {
            Entry(
                    keyOffset = s.readS16_le(),
                    unknown = s.readU8(),
                    dataType = s.readU8(),
                    valueSize = s.readS32_le(),
                    valueSizePad = s.readS32_le(),
                    valueOffset = s.readS32_le()
            )
        }

        val skey = s.slice(keyTable)
        val svalue = s.slice(valueTable)

        return entries.map { entry ->
            val key = skey.slice(entry.keyOffset).readStringz()
            val values = svalue.slice(entry.valueOffset until entry.valueOffset + entry.valueSize)
            val value: Any = when (entry.dataType) {
                DataType.Binary -> values.readAll()
                DataType.Int -> values.readS32_le()
                DataType.Text -> values.readStringz()
                else -> Unit
            }
            key to value
        }.toMap()

        /*
		{ magic: UInt32 },
		{ version: UInt32 },
		{ keyTable: UInt32 },
		{ valueTable: UInt32 },
		{ numberOfPairs: UInt32 },
		         */
    }

    object DataType {
        const val Binary = 0
        const val Text = 2
        const val Int = 4
    }

}