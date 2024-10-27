package me.mason.ktttf.graphics

const val C = 0

// Vertical
const val T = 1
const val B = 2

// Horizontal
const val L = 1
const val R = 2

// Alignments
const val TL = (T shl 2) or L
const val TC = T shl 2
const val TR = (T shl 2) or R

const val CL = L
const val CC = 0
const val CR = R

const val BL = (B shl 2) or L
const val BC = B shl 2
const val BR = (B shl 2) or R

val Int.x get() = this and 0b11
val Int.y get() = this shr 2