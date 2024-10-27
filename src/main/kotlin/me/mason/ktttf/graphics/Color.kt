package me.mason.ktttf.graphics

fun rgba(r: Int, g: Int, b: Int, a: Int) =
    (r and 0xFF shl 24) or (g and 0xFF shl 16) or (b and 0xFF shl 8) or (a and 0xFF)

val Int.r get() = this shr 24
val Int.g get() = (this shr 16) and 0xFF
val Int.b get() = (this shr 8) and 0xFF
val Int.a get() = this and 0xFF