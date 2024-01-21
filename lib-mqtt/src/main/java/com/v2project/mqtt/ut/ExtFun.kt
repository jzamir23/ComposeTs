@file:Suppress("unused")

package com.v2project.mqtt.ut

import com.soywiz.kds.BitSet
import kotlin.experimental.or
import kotlin.math.ceil

/* Byte转Int */
fun Byte.toTrueInt(): Int = this.toInt().and(0xFF)

/* UByte转Int */
fun UByte.toTrueInt(): Int = this.toInt().and(0xFF)

/* Byte转Hex字符串 */
fun Byte.toHex(): String {
    return String.format("%02X", this.toTrueInt())
}

/* UByte转Hex字符串 */
fun UByte.toHex(): String {
    return String.format("%02X", this.toTrueInt())
}

/* ByteArray转Hex字符串 */
fun ByteArray.toHex(pair: Pair<Int, Int>? = null): String {
    return if (pair == null) {
        Utils.toHex(this)
    } else {
        Utils.toHex(this, pair.first, pair.second)
    }
}

/* ByteArray转Hex字符串,不带空格分割 */
fun ByteArray.toHexNoSpace(pair: Pair<Int, Int>? = null): String {
    return if (pair == null) {
        Utils.toHexNoSpace(this)
    } else {
        Utils.toHexNoSpace(this, pair.first, pair.second)
    }
}

/* ByteArray转Hex字符串,不带空格分割 */
fun ByteArray.toReversedHexNoSpace(pair: Pair<Int, Int>? = null): String {
    return if (pair == null) {
        Utils.toHexNoSpace(this.reversedArray())
    } else {
        Utils.toHexNoSpace(this.copyOfRange(pair.first, pair.second).reversedArray())
    }
}

/* IntArray转Hex字符串 */
fun IntArray.toHex(pair: Pair<Int, Int>? = null): String {
    return if (pair == null) {
        map { it.toHex() }.toString()
    } else {
        this.copyOfRange(pair.first, pair.second).map { it.toHex() }.toString()
    }
}

fun IntArray.toMineString(): String {
    return this.map {
        it.toString()
    }.toString()
}

/* Byte转Long */
fun Byte.toTrueLong(): Long = this.toLong().and(0xFF)

/* Byte起始count个Bit转Int */
fun Byte.sumOfFirst(count: Int): Int {
    val an = when (count) {
        1 -> 0b00000001
        2 -> 0b00000011
        3 -> 0b00000111
        4 -> 0b00001111
        5 -> 0b00011111
        6 -> 0b00111111
        7 -> 0b01111111
        8 -> 0b11111111
        else -> throw Throwable("wrong parameters")
    }
    return this.toTrueInt().and(an)
}

/* Byte末尾count个Bit转Int */
fun Byte.sumOfLast(count: Int): Int {
    if (count > 8 || count < 1) throw Throwable("wrong parameters")
    return this.toTrueInt().shr(8 - count)
}

/* Byte内bit计算 */
fun Byte.sumOf(source: Pair<Int, Int>): Int {
    val an = when (source.second - source.first) {
        1 -> 0b00000001
        2 -> 0b00000011
        3 -> 0b00000111
        4 -> 0b00001111
        5 -> 0b00011111
        6 -> 0b00111111
        7 -> 0b01111111
        8 -> 0b11111111
        else -> throw Throwable("wrong parameters")
    }
    return this.toTrueInt().shr(8 - source.second).and(an)
}

/* Byte转Hex字符串 */
fun Byte.toHexString(): String {
    return String.format("%02X", this.toTrueInt())
}

/* Byte中Bit位转Boolean */
fun Byte.bitToBoolean(index: Int): Boolean {
    return when (index) {
        7 -> toInt().and(0b10000000) == 0b10000000
        6 -> toInt().and(0b01000000) == 0b01000000
        5 -> toInt().and(0b00100000) == 0b00100000
        4 -> toInt().and(0b00010000) == 0b00010000
        3 -> toInt().and(0b00001000) == 0b00001000
        2 -> toInt().and(0b00000100) == 0b00000100
        1 -> toInt().and(0b00000010) == 0b00000010
        0 -> toInt().and(0b00000001) == 0b00000001
        else -> throw Throwable("wrong parameters")
    }
}

fun Byte.hasBitSet(index: Int): Boolean {
    return if (index > 8) false else ((this.toInt() ushr index) and 1) != 0
}

fun ByteArray.toBitSet(): BitSet {
    val bitSet = BitSet(this.size * 8)
    for (i in 0 until this.size) {
        for (j in 0 until 8) {
            bitSet[i * 8 + j] = this[i].hasBitSet(j)
        }
    }
    return bitSet
}

fun BitSet.toByteArray(): ByteArray {
    val bytes = ByteArray(ceil(size / 8.0).toInt())
    for (i in indices) {
        if (this[i]) {
            bytes[i / 8] = bytes[i / 8] or (0x01 shl (i % 8)).toByte()
        }
    }
    return bytes
}

fun BitSet.toBinaryString(): String {
    var string = ""
    this.forEach {
        string += if (it) "1" else "0"
    }
    return string
}

fun BitSet.toBoolString(): String {
    var string = ""
    for (i in this.indices) {
        string += this[i].toString()
        if (i != this.size - 1) {
            string += " "
        }
    }
    return string
}

/**
 * 跨字节bit计算
 *
 * 第fir.first字节的后fir.second位
 * 与第sed.first的前sed.second位
 */
fun ByteArray.sumOfBit(fir: Pair<Int, Int>, sed: Pair<Int, Int>): Int {
    return this[fir.first].sumOfLast(fir.second) + this[sed.first].sumOfFirst(sed.second).shl(fir.second)
}

/* Byte数组倒叙累加 Int */
fun ByteArray.versaToInt(): Int {
    return this.versaToInt(0 to this.size)
}

/* Byte数组倒叙累加 Int */
fun ByteArray.versaToInt(fromTo: Pair<Int, Int>): Int {
    return (fromTo.first until fromTo.second).sumOf {
        this[it].toTrueInt().shl((it - fromTo.first) * 8)
    }
}

/* Byte数组倒叙累加 Long */
fun ByteArray.versaToLong(fromTo: Pair<Int, Int>): Long {
    return (fromTo.first until fromTo.second).sumOf {
        this[it].toTrueLong().shl((it - fromTo.first) * 8)
    }
}

/* ByteArray转Hex字符串 */
fun ByteArray.toHex(): String {
    return Utils.toHex(this)
}

/* 将不带空格分割的16进制表示的字符串转换为二进制数组 */
fun String.noSpaceToHex(): ByteArray {
    return Utils.fromHexNoSpace(this)
}

/* 将不带空格分割的16进制表示的字符串转换为反转二进制数组 */
fun String.noSpaceToReversedHex(): ByteArray {
    return Utils.fromHexNoSpace(this).reversedArray()
}

/* 带空格分割的16进制表示的字符串转换为二进制数组 */
fun String.toHex(): ByteArray {
    return Utils.fromHex(this)
}

/* 带空格分割的16进制表示的字符串转换为二进制数组 */
fun String.toReversedHex(): ByteArray {
    return Utils.fromHex(this).reversedArray()
}

/* 计算 Byte数组校验和 */
fun ByteArray.checksum(fromIndex: Int = 0, toIndex: Int = this.size): Int {
    var cs = 0
    if (fromIndex > toIndex || toIndex > size) {
        throw Throwable("参数错误")
    }
    (fromIndex until toIndex).forEach {
        cs += this[it].toInt()
    }
    return cs.and(0xFF)
}

/* 逗号分割的16进制字符串转ByteArray */
fun String.hexToByteArray(): ByteArray {
    if (this.isEmpty()) {
        return ByteArray(0)
    }
    return this.replace("[", "").replace("]", "").split(", ")
        .map { it.toInt(16).toByte() }.toByteArray()
}

/* Parses the string as a [Double] number and returns the result. */
fun String.toDoubleOrZero(): Double {
    try {
        return this.toDouble()
    } catch (e: NumberFormatException) {
        e.printStackTrace()
    }
    return 0.0
}

/* LinkedHashMap转String */
fun LinkedHashMap<String, String>.toMineString(): String {
    val builder = StringBuilder()
    this.forEach {
        builder.append("${it.key}  :  ${it.value}\n")
    }
    return builder.toString()
}

/* Int转换为ByteArray */
fun Int.toBytes(len: Int): ByteArray {
    return ByteArray(len) {
        shr(it * 8).and(0xFF).toByte()
    }
}

fun Long.toBytes(len: Int): ByteArray {
    return ByteArray(len) {
        toInt().shr(it * 8).and(0xFF).toByte()
    }
}

fun Int.toHex(): String {
    return String.format("%02X", this)
}

fun Int.toHexString(): String {
    return this.toUInt().toString(16).uppercase()
}

/* 将给定的小数四舍五入到指定的小数位 */
fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

/* 将给定的小数四舍五入到指定的小数位 */
fun Float.round(decimals: Int): Float {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return (kotlin.math.round(this * multiplier) / multiplier).toFloat()
}

/* 是否是无效值 */
fun Int.invalid(): Boolean {
    return this == 0x80000000.toInt()
}

/* 是否是有效值 */
fun Int.valid(): Boolean {
    return this != 0x80000000.toInt()
}

/* 有效或0 */
fun Int.validOrZero(): Int {
    return if (this.invalid()) 0 else this
}

/* 有效或NULL */
fun Int.validOrNull(): Int? {
    return if (this.invalid()) null else this
}

/* 是否是无效值 */
fun Short.invalid(): Boolean {
    return this == 0x8000.toShort()
}

/* 是否是有效值 */
fun Short.valid(): Boolean {
    return this != 0x8000.toShort()
}

/* 有效或0 */
fun Short.validOrZero(): Short {
    return if (this.invalid()) 0 else this
}

/* 有效值或NULL */
fun Short.validOrNull(): Short? {
    return if (this.invalid()) null else this
}

fun UByte.bitOfBool(offset: Int): Boolean {
    return (this.toInt().shr(offset) and 0xFF) == 1
}

fun UByte.bitsOfValue(pair: Pair<Int, Int>): UByte {
    return (this.toInt().shr(pair.first) and 0xFF).toUByte()
}

fun Int.bitOfBool(offset: Int): Boolean {
    return (this.shr(offset) and 0xFF) == 1
}

fun DateTime.string(): String {
    return date2String(this)
}

