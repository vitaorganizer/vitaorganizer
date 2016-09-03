package com.soywiz.util

abstract class ColorFormat {
    abstract fun getR(v: Int): Int
    abstract fun getG(v: Int): Int
    abstract fun getB(v: Int): Int
    abstract fun getA(v: Int): Int

    fun getRf(v: Int): Float = getR(v).toFloat() / 255f
    fun getGf(v: Int): Float = getG(v).toFloat() / 255f
    fun getBf(v: Int): Float = getB(v).toFloat() / 255f
    fun getAf(v: Int): Float = getA(v).toFloat() / 255f

    //@JTranscMethodBody(target = "js", value = "return Math.min(Math.max(p0, 0), 255);")
    fun clamp0_FF(a: Int): Int = Math.min(Math.max(a, 0), 255)

    //@JTranscMethodBody(target = "js", value = "return Math.min(p0, 255);")
    fun clampFF(a: Int): Int = Math.min(a, 255)

    fun toRGBA(v: Int) = RGBA.packFast(getR(v), getG(v), getB(v), getA(v))
}

object RGBA : ColorFormat() {
    //private inline val R_SHIFT: Int get() = 0
    //private inline val G_SHIFT: Int get() = 8
    //private inline val B_SHIFT: Int get() = 16
    //private inline val A_SHIFT: Int get() = 24

    override fun getR(v: Int): Int = (v ushr 0) and 0xFF
    override fun getG(v: Int): Int = (v ushr 8) and 0xFF
    override fun getB(v: Int): Int = (v ushr 16) and 0xFF
    override fun getA(v: Int): Int = (v ushr 24) and 0xFF

    //fun getRGB(v: Int): Int = v and 0xFFFFFF

    @JvmStatic fun getRGB(v: Int): Int = v and 0xFFFFFF

    @JvmStatic fun packFast(r: Int, g: Int, b: Int, a: Int) = (r shl 0) or (g shl 8) or (b shl 16) or (a shl 24)
    @JvmStatic fun packfFast(r: Float, g: Float, b: Float, a: Float): Int = ((r * 255).toInt() shl 0) or ((g * 255).toInt() shl 8) or ((b * 255).toInt() shl 16) or ((a * 255).toInt() shl 24)

    @JvmStatic fun pack(r: Int, g: Int, b: Int, a: Int) = ((clamp0_FF(r)) shl 0) or ((clamp0_FF(g)) shl 8) or ((clamp0_FF(b)) shl 16) or ((clamp0_FF(a)) shl 24)

    @JvmStatic fun packRGB_A(rgb: Int, a: Int): Int = (rgb and 0xFFFFFF) or (a shl 24)

    @JvmStatic fun blend(c1: Int, c2: Int, factor: Int): Int {
        val f1 = 256 - factor
        return ((
                ((((c1 and 0xFF00FF) * f1) + ((c2 and 0xFF00FF) * factor)) and 0xFF00FF00.toInt())
                        or
                        ((((c1 and 0x00FF00) * f1) + ((c2 and 0x00FF00) * factor)) and 0x00FF0000))) ushr 8
    }

    @JvmStatic operator fun invoke(r: Int, g: Int, b: Int, a: Int) = pack(r, g, b, a)

    @JvmStatic fun rgbaToBgra(v: Int) = ((v shl 16) and 0x00FF0000) or ((v shr 16) and 0x000000FF) or (v and 0xFF00FF00.toInt())

    //@JTranscMethodBody(target = "js", value = "return (p0 < 0) ? 0 : ((p0 > 1) ? 255 : (p0 * 255));")
    @JvmStatic private fun f2i(v: Float): Int = (Mathf.clamp(v, 0f, 1f) * 255).toInt()

    @JvmStatic fun packf(r: Float, g: Float, b: Float, a: Float): Int = packFast(f2i(r), f2i(g), f2i(b), f2i(a))
    @JvmStatic fun packf(rgb: Int, a: Float): Int = packRGB_A(rgb, f2i(a))
}

class BColor(
        @JvmField var r: Float,
        @JvmField var g: Float,
        @JvmField var b: Float,
        @JvmField var a: Float
) {
    constructor(c: BColor) : this(c.r, c.g, c.b, c.a)
    constructor() : this(0f, 0f, 0f, 0f)

    fun set(r: Float, g: Float, b: Float, a: Float) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }

    fun add(r: Float, g: Float, b: Float, a: Float) {
        this.r += r
        this.g += g
        this.b += b
        this.a += a
    }

    fun set(c: BColor) = set(c.r, c.g, c.b, c.a)
}

object RGBA_4444 : ColorFormat() {
    override fun getR(v: Int): Int = (((v ushr 0) and 0xF) * 0xFF) / 0xF
    override fun getG(v: Int): Int = (((v ushr 4) and 0xF) * 0xFF) / 0xF
    override fun getB(v: Int): Int = (((v ushr 8) and 0xF) * 0xFF) / 0xF
    override fun getA(v: Int): Int = (((v ushr 12) and 0xF) * 0xFF) / 0xF
}