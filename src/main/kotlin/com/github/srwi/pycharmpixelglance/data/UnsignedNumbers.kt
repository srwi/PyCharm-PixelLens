package com.github.srwi.pycharmpixelglance.data

import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.map

class MyUInt(private val value: Int) : Comparable<MyUInt> {
    fun toInt(): Int = value
    fun toUInt(): UInt = value.toUInt()

    operator fun plus(other: MyUInt): MyUInt = MyUInt((this.toUInt() + other.toUInt()).toInt())
    operator fun minus(other: MyUInt): MyUInt = MyUInt((this.toUInt() - other.toUInt()).toInt())
    operator fun times(other: MyUInt): MyUInt = MyUInt((this.toUInt() * other.toUInt()).toInt())
    operator fun div(other: MyUInt): MyUInt = MyUInt((this.toUInt() / other.toUInt()).toInt())

    override fun compareTo(other: MyUInt): Int = this.toUInt().compareTo(other.toUInt())
    override fun toString(): String = toUInt().toString()

    companion object {
        fun fromInt(value: Int): MyUInt = MyUInt(value)
        fun fromUInt(value: UInt): MyUInt = MyUInt(value.toInt())
    }
}

inline fun <reified D : Dimension> Multik.ndarrayMyUInt(list: List<MyUInt>, shape: IntArray): NDArray<MyUInt, D> {
    return this.ndarray<Int, D>(list.map { it.toInt() }, shape).map { MyUInt(it) }
}