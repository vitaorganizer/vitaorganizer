package com.soywiz.util

object Mathf {
    @JvmStatic fun clamp(v: Float, min: Float, max: Float) = if (v < min) min else if (v > max) max else v
    @JvmStatic fun clamp(v: Double, min: Double, max: Double) = if (v < min) min else if (v > max) max else v

    @JvmStatic fun floor(v: Float) = Math.floor(v.toDouble()).toFloat()
    @JvmStatic fun round(v: Float) = Math.round(v.toDouble()).toFloat()
    @JvmStatic fun ceil(v: Float) = Math.ceil(v.toDouble()).toFloat()

    @JvmStatic fun transform01(v: Float, b0: Float, b1: Float) = (v * (b1 - b0)) + b0
    @JvmStatic fun transform(v: Float, a0: Float, a1: Float, b0: Float, b1: Float) = transform01((v - a0) / (a1 - a0), b0, b1)

    @JvmStatic fun transform01(v: Double, b0: Double, b1: Double) = (v * (b1 - b0)) + b0
    @JvmStatic fun transform(v: Double, a0: Double, a1: Double, b0: Double, b1: Double) = transform01((v - a0) / (a1 - a0), b0, b1)
}