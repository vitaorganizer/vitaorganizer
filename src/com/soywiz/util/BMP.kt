package com.soywiz.util

object BMP : ImageFormat() {
    override fun check(s: Stream2): Boolean = s.readStringz(2) == "BM"

    override fun read(s: Stream2): Bitmap {
        if (s.readStringz(2) != "BM") invalidOp("Not a BMP file")
        // FILE HEADER
        val size = s.readS32_le()
        val reserved1 = s.readS16_le()
        val reserved2 = s.readS16_le()
        val offBits = s.readS32_le()
        // INFO HEADER
        val bsize = s.readS32_le()
        val width = s.readS32_le()
        val height = s.readS32_le()
        val planes = s.readS16_le()
        val bitcount = s.readS16_le()
        val compression = s.readS32_le()
        val sizeImage = s.readS32_le()
        val pixelsPerMeterX = s.readS32_le()
        val pixelsPerMeterY = s.readS32_le()
        val clrUsed = s.readS32_le()
        val clrImportant = s.readS32_le()

        if (bitcount == 8) {
            val out = Bitmap8(width, height)
            for (n in 0 until 256) out.palette[n] = s.readS32_le() or 0xFF000000.toInt()
            for (n in 0 until height) out.setRow(height - n - 1, s.readBytes(width))
            return out
        } else {
            val out = Bitmap32(width, height)
            for (n in 0 until height) out.setRow(height - n - 1, s.readIntArray_le(width))
            return out
        }
    }
}