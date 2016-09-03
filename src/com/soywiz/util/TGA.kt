package com.soywiz.util

object TGA : ImageFormat() {
    override fun check(s: Stream2): Boolean {
        try {
            readHeader(s)
            return true
        } catch (t: Throwable) {
            return false
        }
    }

    class Info(
            val width: Int,
            val height: Int,
            val flipY: Boolean
    )

    // http://www.paulbourke.net/dataformats/tga/
    fun readHeader(s: Stream2): Info {
        val idLength = s.readU8()
        val colorMapType = s.readU8()
        val imageType = s.readU8()
        when (imageType) {
            1 -> invalidOp("Unsupported indexed")
            2 -> Unit // RGBA
            9, 10 -> invalidOp("Unsupported RLE")
            else -> invalidOp("Unknown TGA")
        }
        val firstIndexEntry = s.readU16_le()
        val colorMapLength = s.readU16_le()
        val colorMapEntrySize = s.readU8()
        s.position += colorMapLength * colorMapEntrySize
        val xorig = s.readS16_le()
        val yorig = s.readS16_le()
        val width = s.readS16_le()
        val height = s.readS16_le()
        val pixelDepth = s.readU8()
        when (pixelDepth) {
            24, 32 -> Unit
            else -> invalidOp("Not a RGBA tga")
        }
        val imageDescriptor = s.readU8()
        val flipY = ((imageDescriptor ushr 5) and 1) == 0
        val storage = ((imageDescriptor ushr 6) and 3)
        s.readBytes(idLength)
        return Info(width = width, height = height, flipY = flipY)
    }

    override fun read(s: Stream2): Bitmap {
        val info = readHeader(s)
        val out = Bitmap32(info.width, info.height)
        for (n in 0 until out.area) out.data[n] = s.readS32_le()
        if (info.flipY) out.flipY()
        return out
    }
}