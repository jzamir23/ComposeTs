package com.v2project.mqtt.ut

import android.util.Log
import com.soywiz.kds.BitSet
import kotlinx.serialization.StringFormat
import java.io.File
import java.io.FileInputStream
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.regex.Pattern
import java.util.zip.CRC32
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.pow

object Utils {
    private const val TAG = "mqttUtils"

    /** 大写字母，严禁修改为小写字母 */
    private val hexMap =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    /**
     * 计算cs
     *
     * @param data   输入数据
     * @param offset 数据起点
     * @param len    数据长度
     * @return cs
     */
    fun checksum(data: ByteArray, offset: Int, len: Int): Int {
        var length = len
        var cs = 0
        if (length + offset > data.size) {
            length = data.size - offset
        }
        for (i in offset until offset + length) {
            cs += data[i]
        }
        return cs and 0xFF
    }

    /**
     * CRC16校验和算法
     *
     * @param data
     * @param offset
     * @param len
     * @return
     */
    fun crc16(data: ByteArray, offset: Int, len: Int): Int {
        var crc = 0xFFFF
        for (i in 0 until len) {
            var dataByte = data[offset + i]
            for (j in 0..7) {
                val crcBit = if (crc and 0x8000 != 0) 1 else 0
                val dataBit = if (dataByte.toInt() and 0x80 != 0) 1 else 0
                crc = crc shl 1
                if (crcBit != dataBit) {
                    crc = crc xor 0x1021
                }
                dataByte = (dataByte.toInt() shl 1).toByte()
            }
        }
        return crc xor 0xFFFF and 0xFFFF
    }

    /**
     * CRC-CCITT (XModem)
     *
     * @param data   数据帧
     * @param offset 偏移量
     * @param len    数据长度
     * @return crc
     */
    fun crcXModem(data: ByteArray, offset: Int, len: Int): Int {
        var crc = 0x00
        val polynomial = 0x1021
        for (i in 0 until len) {
            val b = data[offset + i]
            for (j in 0..7) {
                val bit = b.toInt() shr 7 - j and 1 == 1
                val c15 = crc shr 15 and 1 == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return crc
    }

    /**
     * ABB表中CRC校验使用的算法，遵循CCITT标准。
     *
     * @param processByte
     * @param initCRC
     * @return
     */
    private fun crc16OnlyABB(processByte: Byte, initCRC: Int): Int {
        var proByte = processByte
        var crc = initCRC
        for (i in 8 downTo 1) {
            if (crc and 0x0001 == 1) {
                crc = crc shr 1
                if (proByte.toInt() and 0x01 == 1) {
                    crc = crc or 0x8000
                }
                crc = crc xor 0x8408
                proByte = (proByte.toInt() shr 1).toByte()
            } else {
                crc = crc shr 1
                if (proByte and 0x01 == 1.toByte()) {
                    crc = crc or 0x8000
                }
                proByte = (proByte.toInt() shr 1).toByte()
            }
        }
        crc = crc and 0xFFFF
        return crc
    }

    fun crcForABB(frameByte: ByteArray, length: Int): Int {
        var crc: Int
        crc = frameByte[1].inv().toInt() shl 8 or (frameByte[0].inv().toInt() and 0xFF) and 0xFFFF
        for (i in 2 until length) {
            crc = crc16OnlyABB(frameByte[i], crc)
        }
        crc = crc16OnlyABB(0x00.toByte(), crc)
        crc = crc16OnlyABB(0x00.toByte(), crc)
        crc = crc.inv() and 0xFFFF
        crc = crc shr 8 and 0xFFFF or (crc shl 8 and 0xFFFF)
        return crc
    }

    /**
     * 将二进制数组打印成字符串,带空格分割
     *
     * @param data
     * @param offset
     * @param len
     * @return
     */
    fun toHex(data: ByteArray, offset: Int, len: Int): String {
        var length = len
        if (length + offset > data.size) {
            length = data.size - offset
        }
        val str = CharArray(length * 3)
        for (i in 0 until length) {
            str[i * 3] = hexMap[data[i + offset].toInt() shr 4 and 0xF]
            str[i * 3 + 1] = hexMap[(data[i + offset] and 0xF).toInt()]
            str[i * 3 + 2] = ' '
        }
        return String(str).trimEnd()
    }

    /**
     * 将二进制数组打印成字符串,带空格分割
     *
     * @param data data
     * @return String
     */
    fun toHex(data: ByteArray?): String {
        return if (data == null) "" else toHex(data, 0, data.size)
    }

    /**
     * 将带空格分割的16进制表示的字符串转换为二进制数组
     *
     * @param hexString hexString
     * @return byte[]
     */
    fun fromHex(hexString: String?): ByteArray {
        hexString?.let {
            try {
                var hexStr = hexString
                hexStr = hexStr.trim { it <= ' ' }
                val s = hexStr.split(" ").toTypedArray()
                val ret = ByteArray(s.size)
                for (i in s.indices) {
                    ret[i] = Integer.parseInt(s[i], 16).toByte()
                }
                return ret
            } catch (e: NumberFormatException) {
                Log.e(TAG, e.message.toString())
            }
        }
        return ByteArray(0)
    }

    /**
     * 将二进制数组打印成字符串,不带空格分割
     *
     * @param data   data
     * @param offset offset
     * @param len    len
     * @return
     */
    fun toHexNoSpace(data: ByteArray, offset: Int, len: Int): String {
        var length = len
        if (length + offset > data.size) {
            length = data.size - offset
        }
        val str = CharArray(length shl 1)
        for (i in 0 until length) {
            str[i shl 1] = hexMap[data[i + offset].toInt() shr 4 and 0xF]
            str[(i shl 1) + 1] = hexMap[(data[i + offset] and 0xF).toInt()]
        }
        return String(str)
    }

    /**
     * 将二进制数组打印成字符串,不带空格分割
     *
     * @param data data
     * @return
     */
    fun toHexNoSpace(data: ByteArray): String {
        return toHexNoSpace(data, 0, data.size)
    }

    /**
     * 将二进制数组转换为地址字符串
     */
    fun toAddressString(data: ByteArray): String {
        return toHexNoSpace(data.reversedArray(), 0, data.size)
    }

    /**
     * 将不带空格分割的16进制表示的字符串转换为二进制数组
     *
     * @param hexString hexString
     * @return
     */
    fun fromHexNoSpace(hexString: String?): ByteArray {
        hexString?.let {
            try {
                var hexStr = hexString
                hexStr = hexStr.trim { it <= ' ' }
                val ret = ByteArray((hexStr.length + 1) / 2)
                var i = hexStr.length
                while (i > 0) {
                    ret[(i - 1) / 2] =
                        hexStr.substring(if (i - 2 >= 0) i - 2 else 0, i).toInt(16).toByte()
                    i -= 2
                }
                return ret
            } catch (e: NumberFormatException) {
                Log.e(TAG, e.message.toString())
            }
        }
        return ByteArray(0)
    }

    /**
     * 反转数组
     *
     * @param array array
     * @return 被反转后的临时数组
     */
    fun reverse(array: ByteArray?): ByteArray {
        if (array == null) {
            return byteArrayOf()
        }
        val tmp = ByteArray(array.size)
        for (i in array.indices) {
            tmp[array.size - 1 - i] = array[i]
        }
        return tmp
    }

    /**
     * 反转数组
     */
    fun reverse(array: CharArray): CharArray {
        val tmp = CharArray(array.size)
        for (i in array.indices) {
            tmp[array.size - 1 - i] = array[i]
        }
        return tmp
    }

    /**
     * 反转数组
     *
     * @param array 反转前的原数组
     * @return 被反转后的数组
     */
    fun <T> reverse(array: Array<T>): Array<T> {
        for (i in array.indices) {
            array[array.size - 1 - i] = array[i]
        }
        return array
    }

    /**
     * 为数组的每个元素自加0x33
     *
     * @param data   data
     * @param offset offset
     * @param length length
     */
    fun add(data: ByteArray, value: Int, offset: Int, length: Int) {
        for (i in offset until offset + length) {
            data[i] = (data[i] + value).toByte()
        }
    }

    /**
     * 根据DA1，DA2返回所有的Pn值，以整数方式返回
     *
     * @param DA1
     * @param DA2
     * @return
     */
    fun getPns(DA1: Byte, DA2: Byte): List<Int> {
        val result: MutableList<Int> = ArrayList()
        if (DA1.toInt() == 0 && DA2.toInt() == 0) {
            result.add(0)
        } else {
            for (i in 0..7) {
                if (1 shl i and DA1.toInt() == 1 shl i) {// 该位被设置了
                    if (DA2.toInt() == 0x00) {
                        for (j in 0..254) {
                            result.add(j * 8 + i + 1)
                        }
                    } else {
                        result.add(((DA2.toInt() and 0xFF) - 1) * 8 + i + 1)
                    }
                }
            }
        }
        return result
    }

    /**
     * 把pn转换为DA1，DA2，DA1存放在返回数组的0索引，DA2存放在返回数组的1索引
     *
     * @return
     */
    fun setPn(pn: Int): ByteArray {
        val result = ByteArray(2)
        if (pn == 0) {
            result[0] = 0
            result[1] = 0
        } else {
            result[0] = (1 shl (pn - 1) % 8).toByte()
            result[1] = ((pn - 1) / 8 + 1).toByte()
        }
        return result
    }

    fun setPnBit(pn: Int, data: ByteArray, offset: Int) {
        if (pn != 0) {
            data[offset] = data[offset] or (1 shl (pn - 1) % 8).toByte()
            data[offset + 1] = data[offset + 1] or ((pn - 1) / 8 + 1).toByte()
        }
    }

    /**
     * 把pn转换为DA1，DA2，DA1存放在返回数组的0索引，DA2存放在返回数组的1索引
     *
     * @return
     */
    fun setPn(pn: Int, data: ByteArray, offset: Int) {
        if (pn == 0) {
            data[offset] = 0
            data[offset + 1] = 0
        } else {
            data[offset] = (1 shl (pn - 1) % 8).toByte()
            data[offset + 1] = ((pn - 1) / 8 + 1).toByte()
        }
    }

    /**
     * 根据DT1，DT2返回所有的Fn值，以整数方式返回
     *
     * @param DT1
     * @param DT2
     * @return
     */
    fun getFns(DT1: Byte, DT2: Byte): List<Int> {
        val result: MutableList<Int> = ArrayList()
        for (i in 0..7) {
            if (1 shl i and DT1.toInt() == 1 shl i) {// 该位被设置了
                if (DT2.toInt() and 0xFF == 0xFF) {
                    for (j in 0..30) {
                        result.add(j * 8 + i + 1)
                    }
                } else {
                    result.add((DT2.toInt() and 0xFF) * 8 + i + 1)
                }
            }
        }
        return result
    }

    /**
     * 把pn转换为DT1，DT2，DT1存放在返回数组的0索引，DT2存放在返回数组的1索引
     *
     * @return
     */
    fun setFn(fn: Int): ByteArray {
        val result = ByteArray(2)
        if (fn != 0) {
            result[0] = (1 shl (fn - 1) % 8).toByte()
            result[1] = ((fn - 1) / 8).toByte()
        } else {
            Log.e(TAG, "fn不可能为0")
        }
        return result
    }

    fun setFnBit(fn: Int, dt: ByteArray, offset: Int) {
        if (fn != 0) {
            dt[0 + offset] = dt[0 + offset] or (1 shl (fn - 1) % 8).toByte()
            dt[1 + offset] = dt[1 + offset] or ((fn - 1) / 8).toByte()
        } else {
            Log.e(TAG, "fn不可能为0")
        }
    }

    /**
     * 把pn转换为DT1，DT2，DT1存放在返回数组的offset索引，DT2存放在返回数组的offset+1索引
     *
     * @return
     */
    fun setFn(fn: Int, data: ByteArray, offset: Int) {
        if (fn != 0) {
            data[offset] = (1 shl (fn - 1) % 8).toByte()
            data[offset + 1] = ((fn - 1) / 8).toByte()
        } else {
            Log.e(TAG, "fn不可能为0")
        }
    }

    fun A1(data: ByteArray): String {
        val weekName = arrayOf("X", "一", "二", "三", "四", "五", "六", "日")
        return String.format(
            "%02x-%02x-%02x %02x:%02x:%02x 周%s",
            data[5],
            data[4] and 0x1F,
            data[3],
            data[2],
            data[1],
            data[0],
            weekName[data[4].toInt() shr 5 and 0x07]
        )
    }

    fun A12(data: ByteArray): String {
        return String.format(
            "%02x%02x%02x%02x%02x%02x",
            data[5].toInt() and 0xFF,
            data[4].toInt() and 0xFF,
            data[3].toInt() and 0xFF,
            data[2].toInt() and 0xFF,
            data[1].toInt() and 0xFF,
            data[0].toInt() and 0xFF
        )
    }

    fun A14(data: ByteArray): String {
        return String.format(
            "%02x%02x%02x.%02x%02xkWh",
            data[4],
            data[3],
            data[2],
            data[1],
            data[0]
        )
    }

    fun A20(data: ByteArray): String {
        return String.format("%02x年%02x月%02x日", data[2], data[1], data[0])
    }

    /**
     * 二进制转换成16进制
     *
     * @param bString bString
     * @return String
     */
    fun binaryString2hexString(bString: String?): String? {
        if (bString == null || bString == "" || bString.length % 8 != 0) return null
        val tmp = StringBuilder()
        var iTmp: Int
        var i = 0
        while (i < bString.length) {
            iTmp = 0
            for (j in 0..3) {
                iTmp += Integer.parseInt(bString.substring(i + j, i + j + 1)) shl 4 - j - 1
            }
            tmp.append(Integer.toHexString(iTmp))
            i += 4
        }
        return tmp.toString()
    }

    /**
     * 十六进制转换成二进制
     */
    fun binaryString(data: Byte): String {
        val s = StringBuilder()
        if (Integer.toBinaryString(byte2ten(data)).length < 8) {
            for (i in 1..8 - Integer.toBinaryString(byte2ten(data)).length) {
                s.append("0")
            }
        }
        return s.toString() + Integer.toBinaryString(byte2ten(data))
    }

    /**
     * 十六进制转换成二进制字符串
     */
    fun binaryString(data: ByteArray): String {
        val s = StringBuilder()
        for (aData in data) {
            s.append(binaryString(aData))
        }
        return s.toString()
    }

    /**
     * 二进制转换成十进制
     */
    fun binaryString2ten(data: String): String {
        return BigInteger(data, 2).toString()
    }

    /**
     * byte转换成10进制
     */
    private fun byte2ten(data: Byte): Int {
        return data.toInt() and 0xFF
    }

    /**
     * 左(前)补值
     * @param str
     * @param strLength
     * @return str
     */
    fun addZeroForNum(str: String, strLength: Int, value: String = "0"): String {
        var string = str
        var strLen = string.length
        var sb: StringBuffer?
        while (strLen < strLength) {
            sb = StringBuffer()
            sb.append(value).append(string)
            string = sb.toString()
            strLen = string.length
        }
        return string
    }

    /**
     * 字符串后面加0
     */
    fun addZero(str: String, strLength: Int, value: String = "0"): String {
        var string = str
        var strLen = string.length
        var sb: StringBuffer?
        while (strLen < strLength) {
            sb = StringBuffer()
            sb.append(string).append(value)// 右(后)补0
            string = sb.toString()
            strLen = string.length
        }
        return string
    }

    /**
     * 低位在前，高位在后，将两个字节数据value添加到数组data的offset索引处
     *
     * @param data
     * @param offset
     * @param value
     */
    fun setShort(data: ByteArray, offset: Int, value: Short) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = (value.toInt() shr 8 and 0xFF).toByte()
    }

    /**
     * 高位在前，低位在后，将两个字节数据value添加到数组data的offset索引处
     */
    fun setBigEndianShort(data: ByteArray, offset: Int, value: Short) {
        data[offset + 1] = (value and 0xFF).toByte()
        data[offset] = (value.toInt() shr 8 and 0xFF).toByte()
    }

    /**
     * 高位在前，低位在后，将两个字节数据value添加到数组data的offset索引处
     */
    fun setBigEndianShort(data: ByteArray, offset: Int, value: UShort) {
        data[offset + 1] = (value.toInt() and 0xFF).toByte()
        data[offset] = (value.toInt() shr 8 and 0xFF).toByte()
    }

    /**
     * 低位在前，高位在后，将两个字节数据value从数组data的offset索引处读取出来，返回无符号值
     *
     * @param data
     * @param offset
     * @return
     */
    fun getUShort(data: ByteArray, offset: Int): UShort {
        return ((data[offset].toInt() and 0xFF) or (data[offset + 1].toInt() and 0xFF shl 8)).toUShort()
    }

    /**
     * 低位在前，高位在后，将两个字节数据value从数组data的offset索引处读取出来，返回有符号值
     *
     * @param data
     * @param offset
     * @return
     */
    fun getShort(data: ByteArray, offset: Int): Short {
        return ((data[offset].toInt() and 0xFF) or (data[offset + 1].toInt() and 0xFF shl 8)).toShort()
    }

    /**
     * 高位在前，低位在后，将两个字节数据value从数组data的offset索引处读取出来
     *
     * @param data
     * @param offset
     * @return
     */
    fun getBigEndianShort(data: ByteArray, offset: Int): Int {
        return (data[offset + 1].toInt() and 0xFF) or (data[offset].toInt() and 0xFF shl 8)
    }


    /**
     * 低位在前，高位在后，将4个字节数据value添加到数组data的offset索引处
     *
     * @param data
     * @param offset
     * @param value
     */
    fun setInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = (value shr 8 and 0xFF).toByte()
        data[offset + 2] = (value shr 16 and 0xFF).toByte()
        data[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    /**
     * 低位在前，高位在后，将4个字节数据value从数组data的offset索引处读取出来，返回无符号值
     *
     * @param data
     * @param offset
     * @return
     */
    fun getUInt(data: ByteArray, offset: Int): UInt {
        return ((data[offset].toInt() and 0xFF or (data[offset + 1].toInt() and 0xFF shl 8) or (data[offset + 2].toInt() and 0xFF shl 16) or (data[offset + 3].toInt() and 0xFF shl 24)).toUInt())
    }

    /**
     * 低位在前，高位在后，将4个字节数据value从数组data的offset索引处读取出来，返回有符号值
     *
     * @param data
     * @param offset
     * @return
     */
    fun getInt(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF or (data[offset + 1].toInt() and 0xFF shl 8) or (data[offset + 2].toInt() and 0xFF shl 16) or (data[offset + 3].toInt() and 0xFF shl 24))
    }

    /**
     * 低位在前，高位在后，将8个字节数据value添加到数组data的offset索引处
     *
     * @param data   填充的内存
     * @param offset 偏移量
     * @param value  long value
     */
    fun setLong(data: ByteArray, offset: Int, value: Long) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = (value shr 8 and 0xFF).toByte()
        data[offset + 2] = (value shr 16 and 0xFF).toByte()
        data[offset + 3] = (value shr 24 and 0xFF).toByte()
        data[offset + 4] = (value shr 32 and 0xFF).toByte()
        data[offset + 5] = (value shr 40 and 0xFF).toByte()
        data[offset + 6] = (value shr 48 and 0xFF).toByte()
        data[offset + 7] = (value shr 56 and 0xFF).toByte()
    }

    /**
     * 低位在后，高位在前，将8个字节数据value添加到数组data的offset索引处
     *
     * @param data   填充的内存
     * @param offset 偏移量
     * @param value  long value
     */
    fun setBigEndianLong(data: ByteArray, offset: Int, value: Long) {
        data[offset + 7] = (value and 0xFF).toByte()
        data[offset + 6] = (value shr 8 and 0xFF).toByte()
        data[offset + 5] = (value shr 16 and 0xFF).toByte()
        data[offset + 4] = (value shr 24 and 0xFF).toByte()
        data[offset + 3] = (value shr 32 and 0xFF).toByte()
        data[offset + 2] = (value shr 40 and 0xFF).toByte()
        data[offset + 1] = (value shr 48 and 0xFF).toByte()
        data[offset] = (value shr 56 and 0xFF).toByte()
    }

    fun setBigEndianInt(data: ByteArray, offset: Int, value: Int) {
        data[offset + 3] = (value shr 32 and 0xFF).toByte()
        data[offset + 2] = (value shr 40 and 0xFF).toByte()
        data[offset + 1] = (value shr 48 and 0xFF).toByte()
        data[offset] = (value shr 56 and 0xFF).toByte()
    }

    /**
     * 低位在后，高位在前，将8个字节数据value从数组data的offset索引处读取出来
     *
     * @param data   data
     * @param offset offset
     * @return long
     */
    fun getBigEndianLong(data: ByteArray, offset: Int): Long {
        return (data[offset + 7].toLong() and 0xFF or ((data[offset + 6].toLong() and 0xFF) shl 8) or ((data[offset + 5].toLong() and 0xFF) shl 16) or ((data[offset + 4].toLong() and 0xFF) shl 24) or ((data[offset + 3].toLong() and 0xFF) shl 32) or ((data[offset + 2].toLong() and 0xFF) shl 40) or ((data[offset + 1].toLong() and 0xFF) shl 48) or ((data[offset].toLong() and 0xFF) shl 56))
    }

    fun getBigEndianInt(data: ByteArray, offset: Int): Int {
        return (data[offset + 3].toInt() and 0xFF or ((data[offset + 2].toInt() and 0xFF) shl 8) or ((data[offset + 1].toInt() and 0xFF) shl 16) or ((data[offset].toInt() and 0xFF) shl 24))
    }

    /**
     * 低位在前，高位在后，将8个字节数据value从数组data的offset索引处读取出来
     *
     * @param data
     * @param offset
     * @return
     */
    fun getLong(data: ByteArray, offset: Int): Long {
        return (data[offset].toLong() and 0xFF or ((data[offset + 1].toLong() and 0xFF) shl 8) or ((data[offset + 2].toLong() and 0xFF) shl 16) or ((data[offset + 3].toLong() and 0xFF) shl 24) or ((data[offset + 4].toLong() and 0xFF) shl 32) or ((data[offset + 5].toLong() and 0xFF) shl 40) or ((data[offset + 6].toLong() and 0xFF) shl 48) or ((data[offset + 7].toLong() and 0xFF) shl 56))
    }

    /**
     * 低位在前，高位在后，将3个字节数据value添加到数组data的offset索引处
     *
     * @param data   data
     * @param offset offset
     * @param value  value
     */
    fun set3byte(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = (value shr 8 and 0xFF).toByte()
        data[offset + 2] = (value shr 16 and 0xFF).toByte()
    }

    /**
     * 低位在前，高位在后，将3个字节数据value从数组data的offset索引处读取出来
     *
     * @param data
     * @param offset
     * @return
     */
    fun get3byte(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or (data[offset + 1].toInt() and 0xFF shl 8) or (data[offset + 2].toInt() and 0xFF shl 16)
    }

    /**
     * 低位在前，高位在后，将6个字节数据value添加到数组data的offset索引处
     *
     * @param data
     * @param offset
     * @param value
     */
    fun set6byte(data: ByteArray, offset: Int, value: Long) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = (value shr 8 and 0xFF).toByte()
        data[offset + 2] = (value shr 16 and 0xFF).toByte()
        data[offset + 3] = (value shr 24 and 0xFF).toByte()
        data[offset + 4] = (value shr 32 and 0xFF).toByte()
        data[offset + 5] = (value shr 40 and 0xFF).toByte()
    }

    /**
     * 将bcd码表示的数值（十六进制）转换为十进制，比如bcd码为0x33的数表示的是十进制的33，那么传入的参数bcd为49(0x33)，输出为33
     *
     * @param bcd 传入的bcd码，函数内会搽除该值的高24位，只保留最低的8位（BCD码只有一个字节）
     * @return 十进制结果
     */
    fun bcd2int(bcd: Int): Int {
        var bcd1 = bcd
        bcd1 = bcd1 and 0xFF
        return bcd1 - (bcd1 shr 4) * 6
    }

    /**
     * 将十进制数value转换为bcd码形式，比如说十进制数18,转换为bcd码应当为24（0x18）
     *
     * @param value 输入数据
     * @return 返回的BCD码
     */
    fun int2bcd(value: Int): Int {
        return value / 10 * 6 + value
    }

    /**
     * 将byte数组转换成int数组，调用者需要确保source不为null
     *
     * @param source 输入数组
     * @return 输出数组
     */
    fun bytes2ints(source: ByteArray): IntArray {
        val ret = IntArray(source.size)
        for (i in source.indices) {
            ret[i] = source[i].toInt() and 0xFF
        }
        return ret
    }

    /**
     * 将int数组转换成byte数组，调用者需要确保source不为null
     *
     * @param source 输入数组
     * @return 输出数组
     */
    fun ints2bytes(source: IntArray): ByteArray {
        val ret = ByteArray(source.size)
        for (i in source.indices) {
            ret[i] = source[i].toByte()
        }
        return ret
    }

    fun isAllByte(data: ByteArray, b: Byte): Boolean {
        for (aData in data) {
            if (aData.toInt() and 0xFF != b.toInt() and 0xFf) {
                return false
            }
        }
        return true
    }

    /**
     * 判定字符数组是否包含指定的字符
     *
     * @param data 输入字符数组，b输入的字符
     * @return 如果为全0则返回true
     */
    fun isContainByte(data: ByteArray, b: Byte): Boolean {
        for (aData in data) {
            if (aData.toInt() and 0xFF == b.toInt() and 0xFF) {
                return true
            }
        }
        return false
    }

    /**
     * 移除浮点数字符串前后的0
     *
     * @param string 浮点数字符串
     * @return
     */
    fun removeZeroInDecimal(string: String): String {
        var searchFirstPos = true
        var firstPos = 0
        var lastPos = string.length - 1
        for (i in string.indices) {
            val c = string[i]
            if (searchFirstPos) {
                if (c == '0') {
                    firstPos = i
                } else {
                    searchFirstPos = false
                    lastPos = i
                    if (c != '.') {
                        firstPos = i
                    }
                }
            } else {
                if (c != '0') {
                    lastPos = i
                }
            }
        }
        if (string[lastPos] == '.') {
            lastPos--
        }
        return string.substring(firstPos, lastPos + 1)
    }

    fun removePrefixChar(string: String, ch: Char): String {
        var firstPos = 0
        for (i in string.indices) {
            val c = string[i]
            if (c != ch) {
                firstPos = i
                break
            }
        }
        return string.substring(firstPos)
    }

    /**
     * 从数组中从 0 索引开始搜索指定元素，搜索到第一个元素即返回
     *
     * @param array 待检索数组
     * @param value 待检索的值
     * @param <T>   泛型参数
     * @return -1表示未搜索到
    </T> */
    fun <T> arraySearch(array: Array<T>, value: T): Int {
        for (i in array.indices) {
            val t = array[i]
            if (t == value) {
                return i
            }
        }
        return -1
    }

    /**
     * 从数组中从索引 0 开始搜索指定元素，搜索到第一个元素即返回
     *
     * @param array 待检索数组
     * @param value 待检索的值
     * @return -1表示未搜索到
     */
    fun arraySearch(array: IntArray, value: Int): Int {
        for (i in array.indices) {
            if (array[i] == value) {
                return i
            }
        }
        return -1
    }

    /**
     * 格式化数字大小为带KB/MB/GB/TB的单位，带两位小数
     *
     * @param size 数字大小
     * @return 格式化后的字符串
     */
    fun formatSizeWithUnit(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var tmp = size.toDouble()
        for (unit in units) {
            if (tmp > 1024) {
                tmp /= 1024.0
            } else {
                return String.format("%.2f%s", tmp, unit)
            }
        }
        return String.format("%d%s", size, units[0])
    }

    /*判断一个字节中被置为1的位的个数*/
    fun getNumberOfOneInByte(value: Byte): Int {
        var number = 0
        for (i in 0..7) {
            number += value.toInt() shr i and 1
        }
        return number
    }

    fun getCrc32(data: ByteArray, offset: Int, len: Int): Long {
        val crc32 = CRC32()
        crc32.update(data, offset, len)
        return crc32.value
    }

    fun calcCrc32(data: ByteArray, offset: Int, len: Int): ByteArray {
        val bCrc32 = ByteArray(4)
        val crc32 = getCrc32(data, offset, len)
        int2bytes(crc32.toInt(), bCrc32, 0)
        return bCrc32
    }

    fun getMd5(data: ByteArray?): ByteArray? {
        var md5: MessageDigest? = null
        try {
            md5 = MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, e.message.toString())
        }
        data?.let { md5?.update(it) }
        return md5?.digest()
    }

    fun getFileMD5(file: File): ByteArray? {
        if (!file.isFile) {
            return null
        }
        val digest: MessageDigest
        val fileInputStream: FileInputStream
        val buffer = ByteArray(1024)
        var len: Int
        try {
            digest = MessageDigest.getInstance("MD5")
            fileInputStream = FileInputStream(file)
            while (fileInputStream.read(buffer, 0, 1024).also { len = it } != -1) {
                digest.update(buffer, 0, len)
            }
            fileInputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            return null
        }
        return digest.digest()
    }

    fun bytes2String(data: ByteArray, start: Int, len: Int): String? {
        if (data.size < len + start) {
            Log.e(TAG, "转换的数据有误或超过范围")
            return null
        }
        val s = StringBuilder()
        for (i in 0 until len) {
            s.append(data[start + i])
        }
        return s.toString()
    }

    fun int2bytes(value: Int, data: ByteArray?, offset: Int) {
        if (data == null) {
            Log.e(TAG, "存储data为空")
        } else {
            data[3 + offset] = (value and 0xFF).toByte()
            data[2 + offset] = (value shr 8 and 0xFF).toByte()
            data[1 + offset] = (value shr 16 and 0xFF).toByte()
            data[offset] = (value shr 24 and 0xFF).toByte()
        }
    }

    fun int2bytes(value: Int): ByteArray {
        val bytes = ByteArray(4)
        bytes[3] = (value and 0xFF).toByte()
        bytes[2] = (value shr 8 and 0xFF).toByte()
        bytes[1] = (value shr 16 and 0xFF).toByte()
        bytes[0] = (value shr 24 and 0xFF).toByte()
        return bytes
    }

    fun short2bytes(value: Short): ByteArray {
        val bytes = ByteArray(2)
        bytes[1] = (value and 0xFF).toByte()
        bytes[0] = (value.toInt() shr 8 and 0xFF).toByte()
        return bytes
    }

    fun short2bytes(value: Int, data: ByteArray, offset: Int) {
        data[1 + offset] = (value and 0xFF).toByte()
        data[offset] = (value shr 8 and 0xFF).toByte()
    }

    fun bcdBytes2int(data: ByteArray, start: Int, len: Int, le: Boolean = true): Int {
        if (len > 4 || data.size < len + start) {
            throw NumberFormatException("转换的数据异常 len:$len buffSize:${data.size} start:$start")
        }
        return if (le) {
            toHexNoSpace(data.copyOfRange(start, start + len).reversedArray()).toInt()
        } else {
            toHexNoSpace(data.copyOfRange(start, start)).toInt()
        }
    }

    fun bytes2Long(data: ByteArray, start: Int, len: Int, le: Boolean = true): Long {
        if (len > 8 || data.size < len + start) {
            throw NumberFormatException("转换的数据异常 len:$len buffSize:${data.size} start:$start")
        }
        var num = 0L
        for (i in 0 until len) {
            num += (data[i + start].toInt() and 0xFF).toLong() shl (if (le) i else (len - 1 - i)) * 8
        }
        return num
    }

    fun bytes2Int(data: ByteArray, start: Int, len: Int, le: Boolean = true): Int {
        if (len > 4 || data.size < len + start) {
            throw NumberFormatException("转换的数据异常 len:$len buffSize:${data.size} start:$start")
        }
        var num = 0
        for (i in 0 until len) {
            num += (data[i + start].toInt() and 0xFF) shl (if (le) i else (len - 1 - i)) * 8
        }
        return num
    }

    fun bytes2Short(data: ByteArray, start: Int, len: Int, le: Boolean = true): Short {
        if (len > 2 || data.size < len + start) {
            throw NumberFormatException("转换的数据异常 len:$len buffSize:${data.size} start:$start")
        }
        var num = 0
        for (i in 0 until len) {
            num += (data[i + start].toInt() and 0xFF) shl (if (le) i else (len - 1 - i)) * 8
        }
        return num.toShort()
    }

    fun bcdBytes2Long(data: ByteArray, start: Int, len: Int, le: Boolean = true): Long {
        if (len > 8 || data.size < len + start) {
            throw NumberFormatException("转换的数据异常 len:$len buffSize:${data.size} start:$start")
        }
        return if (le) {
            toHexNoSpace(data.copyOfRange(start, start + len).reversedArray()).toLong()
        } else {
            toHexNoSpace(data.copyOfRange(start, start)).toLong()
        }
    }

    /* Unicode编码处理 */
    fun unicodeHandle(code: ByteArray): ByteArray {
        val ret = ByteArray(code.size)
        for (i in 0 until code.size / 2) {
            ret[i * 2] = code[1 + i * 2]
            ret[1 + i * 2] = code[i * 2]
        }
        return ret
    }

    /**
     * 将数组的 0 删除，用于byte 转 string 时，将里面的NULL删除。
     *
     * @param code   - 待处理的数组
     * @param offset - 偏移量
     * @param len    - 处理字节长度
     * @return byte
     */
    private fun removeNull(code: ByteArray, offset: Int, len: Int): ByteArray {
        val list = ArrayList<Byte>()
        for (i in offset until offset + len) {
            if (code[i] != 0.toByte()) {
                list.add(code[i])
            }
        }
        val data = ByteArray(list.size)
        for (i in list.indices) {
            data[i] = list[i]
        }
        return data
    }

    fun gb2312Str(code: ByteArray, offset: Int, len: Int): String? {
        var bytes = code
        bytes = removeNull(bytes, offset, len)
        var str: String? = null
        try {
            str = String(bytes.copyOfRange(0, bytes.size), Charset.forName("gb2312"))
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, e.message.toString())
        }
        return str
    }

    /**
     * 从字节数组中按照ascii码组成字符串
     *
     * @param data   输入的字节数组
     * @param offset 偏移量
     * @param len    需要处理的长度
     * @return 组成的字符串
     */
    fun ascii(data: ByteArray, offset: Int, len: Int): String {
        val sb = StringBuilder()
        for (i in offset until offset + len) {
            val b = data[i]
            if (b.toInt() == 0) {
                break
            }
            sb.append(b.toInt().toChar())
        }
        return sb.toString()
    }

    /**
     * 裁剪完整的红外根密钥
     *
     * @param content 完整data数据域红外根密钥+mac
     * @return 红外根密钥字符串
     */

    fun getTokens(content: String?): String {
        var i = 0
        var a = 0
        val data = StringBuilder()
        val st = StringTokenizer(content)
        val numTokens = st.countTokens()
        val tokenList = arrayOfNulls<String?>(numTokens)
        while (st.hasMoreElements()) {
            tokenList[i] = st.nextToken()
            i++
        }
        while (a < tokenList.size) {
            if (a != 2 && a < 101) {
                data.append(tokenList[a])
            }
            a++
        }
        return data.toString().trim { it <= ' ' }
    }

    // 当前文件名
    val fileName: String?
        get() {
            return Exception().stackTrace[1].fileName
        }

    // 当前类名
    val className: String
        get() {
            return Exception().stackTrace[1].className
        }

    // 当前方法名
    val methodName: String
        get() {
            return Exception().stackTrace[1].methodName
        }

    // 当前行号
    val lineNumber: Int
        get() {
            return Exception().stackTrace[1].lineNumber
        }

    /**
     * 先存低位字节的叫小端模式  大端模式则先保存高位字节
     * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在后,高位在前)的顺序。
     *
     * @param i 5--->00 00 00 05    和bytesToInt2（）配套使用
     * @return 大端模式
     */
    fun intToByteArray1(i: Int): ByteArray {
        val result = ByteArray(4)
        result[0] = (i shr 24 and 0xFF).toByte()
        result[1] = (i shr 16 and 0xFF).toByte()
        result[2] = (i shr 8 and 0xFF).toByte()
        result[3] = (i and 0xFF).toByte()
        return result
    }

    fun toFloatString(data: Short, decimal: Int): String {
        return String.format(String.format("%%.%df", decimal), data * 10.0.pow(-decimal))
    }

    fun toFloatString(data: Int, decimal: Int): String {
        return String.format(String.format("%%.%df", decimal), data * 10.0.pow(-decimal))
    }

    fun toFloatString(data: Long, decimal: Int): String {
        return String.format(String.format("%%.%df", decimal), data * 10.0.pow(-decimal))
    }

    fun toDouble(data: Short, decimal: Int): Double {
        return data * 10.0.pow(-decimal)
    }

    fun toDouble(data: Int, decimal: Int): Double {
        return data * 10.0.pow(-decimal)
    }

    fun toDouble(data: Long, decimal: Int): Double {
        return data * 10.0.pow(-decimal)
    }

    /**
     * 判断是否包含字母
     *
     * true:包含
     */
    fun containsLetter(msg: String): Boolean {
        val regex = ".*[a-zA-Z]+.*"
        val m = Pattern.compile(regex).matcher(msg)
        return m.matches()
    }

    /**
     * 将芯片代码转换为芯片描述
     */
    fun chipCode2Desc(code: String): String {
        return when (code) {
            "03" -> "X3"
            "13" -> "X3S"
            else -> "X3S"
        }
    }

    /**
     * 设备类型转换为芯片描述
     */
    fun equipmentType2Desc(code: Int): String {
        return when (code) {
            0 -> "未知"
            8 -> "MA1000"
            9 -> "MA2000"
            10 -> "MA2100"
            11 -> "MA1001"
            12 -> "MA2001"
            13 -> "MA2101"
            20 -> "SW250"
            else -> "保留"
        }
    }

    /**
     * 开放组网模式
     */
    fun netMode2Desc(code: Int): String {
        return when (code) {
            0 -> "正常组网"
            1 -> "开放组网"
            else -> "未知"
        }
    }

    /**
     * 城农网类型
     */
    fun netType2Desc(code: Int): String {
        return when (code) {
            0 -> "城网"
            1 -> "农网"
            else -> "未知"
        }
    }

    /**
     * 采集使能标志
     */
    fun cltFlag2Desc(code: Int): String {
        return when (code) {
            0 -> "停止"
            1 -> "启动"
            else -> "未知"
        }
    }

    /**
     * 冻结使能标志
     */
    fun freezeFlag2Desc(code: Int): String {
        return when (code) {
            0 -> "停止"
            1 -> "启动"
            else -> "未知"
        }
    }

    /**
     * 载波搜表选项字 - 模式
     */
    fun plcSearchMode2Desc(code: Int): String {
        return when (code) {
            0 -> "自动"
            1 -> "强制485"
            2 -> "强制PLC"
            else -> "未知"
        }
    }

    @Throws(NumberFormatException::class)
    fun bytes2Float(
        data: ByteArray,
        offset: Int,
        len: Int,
        decimal: Int,
        signed: Boolean = true,
        le: Boolean = true,
    ): Float {
        return if (signed) {       // 有符号
            when (len) {
                2 -> (bytes2Int(data, offset, len, le).toShort() * 10.0.pow(-decimal)).toFloat()
                    .round(decimal)

                4 -> (bytes2Int(data, offset, len, le) * 10.0.pow(-decimal)).toFloat()
                    .round(decimal)

                8 -> (bytes2Long(data, offset, len, le) * 10.0.pow(-decimal)).toFloat()
                    .round(decimal)

                else -> (bytes2Int(data, offset, len, le) * 10.0.pow(-decimal)).toFloat()
                    .round(decimal)
            }
        } else {        // 无符号
            (bytes2Long(data, offset, len, le) * 10.0.pow(-decimal)).toFloat().round(decimal)
        }
    }

    @Throws(NumberFormatException::class)
    fun bytes2Double(
        data: ByteArray,
        offset: Int,
        len: Int,
        decimal: Int,
        signed: Boolean = true,
        le: Boolean = true,
    ): Double {
        return if (signed) {       // 有符号
            when (len) {
                2 -> (bytes2Int(
                    data,
                    offset,
                    len,
                    le
                ).toShort() * 10.0.pow(-decimal)).round(decimal)

                4 -> (bytes2Int(data, offset, len, le) * 10.0.pow(-decimal)).round(decimal)
                8 -> (bytes2Long(data, offset, len, le) * 10.0.pow(-decimal)).round(decimal)
                else -> (bytes2Int(data, offset, len, le) * 10.0.pow(-decimal)).round(decimal)

            }
        } else {        // 无符号
            (bytes2Long(data, offset, len, le) * 10.0.pow(-decimal)).round(decimal)
        }
    }


    /**
     * 二进制字符串转换成 byte[]
     *
     * @param data 输入数据 二进制字符串，如："1001010,1101010,11" -> 0x4A,0x6A,0x03
     * @return byte[]
     */
    fun binaryString2byte(data: String): ByteArray {
        val binaryStrings = data.split(",").toTypedArray()
        val bytes = ByteArray(binaryStrings.size)
        for (i in bytes.indices) {
            bytes[i] = binaryStrings[i].toByte(2)
        }
        return bytes
    }

    /**
     * 获取异常描述字符串，即[Throwable.message]。如果为null，那么就从
     * [Throwable.cause]中获取，以此类推，往上5层终止。
     */
    fun getExceptionMsg(throwable: Throwable?): String? {
        var t = throwable ?: return null
        var msg = t.javaClass.name
        for (i in 0..4) {
            t.message?.let {
                msg = it
                return@let
            } ?: run {
                t.cause?.let {
                    t = it
                }
            }
        }
        return msg
    }


    /**
     * 高位在前，低位在后，将两个字节数据value添加到数组data的offset索引处
     */
    fun setBigEndianShort(data: ByteArray, offset: Int, value: Int) {
        data[offset + 1] = (value and 0xFF).toByte()
        data[offset] = (value shr 8 and 0xFF).toByte()
    }


    /**
     * 将bcd码表示的数值（十六进制）转换为十进制，比如bcd码为0x33的数表示的是十进制的33，那么传入的参数bcd为49(0x33)，输出为33
     *
     * @param bcd 传入的bcd码，函数内会搽除该值的高24位，只保留最低的8位（BCD码只有一个字节）
     * @return 十进制结果
     */
    fun bcd2byte(bcd: Int): Int {
        var bcd1 = bcd
        bcd1 = bcd1 and 0xFF
        return bcd1 - (bcd1 shr 4) * 6
    }

    /**
     * 将十进制数value转换为bcd码形式，比如说十进制数18,转换为bcd码应当为24（0x18）
     *
     * @param value 输入数据
     * @return 返回的BCD码
     */
    fun byte2bcd(value: Int): Int {
        return value / 10 * 6 + value
    }

    /**
     * 判定是否字符串为全0
     *
     * @param str 输入字符串
     * @return 如果为全0则返回true
     */
    fun isAllZero(str: String): Boolean {
        return str.matches(Regex("^0+$"))
    }

    fun int2bytes(value: Int, data: ByteArray?, offset: Int, le: Boolean) {
        if (data == null) {
            Log.e(TAG, "存储data为空")
        } else {
            data[(if (le) 0 else 3) + offset] = (value and 0xFF).toByte()
            data[(if (le) 1 else 2) + offset] = (value shr 8 and 0xFF).toByte()
            data[(if (le) 2 else 1) + offset] = (value shr 16 and 0xFF).toByte()
            data[(if (le) 3 else 0) + offset] = (value shr 24 and 0xFF).toByte()
        }
    }

    fun int2bytes(value: Int, le: Boolean): ByteArray {
        val bytes = ByteArray(4)
        bytes[if (le) 0 else 3] = (value and 0xFF).toByte()
        bytes[if (le) 1 else 2] = (value shr 8 and 0xFF).toByte()
        bytes[if (le) 2 else 1] = (value shr 16 and 0xFF).toByte()
        bytes[if (le) 3 else 0] = (value shr 24 and 0xFF).toByte()
        return bytes
    }

    fun short2bytes(value: Short, le: Boolean): ByteArray {
        val bytes = ByteArray(2)
        if (le) {
            bytes[0] = (value and 0xFF).toByte()
            bytes[1] = (value.toInt() shr 8 and 0xFF).toByte()
        } else {
            bytes[1] = (value and 0xFF).toByte()
            bytes[0] = (value.toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    fun short2bytes(value: Int, data: ByteArray, offset: Int, le: Boolean) {
        data[if (le) offset else 1 + offset] = (value and 0xFF).toByte()
        data[if (le) 1 + offset else offset] = (value shr 8 and 0xFF).toByte()
    }


    fun unicodeStr(code: ByteArray, offset: Int, len: Int): String {
        var str = toHex(code, offset, len)
        try {
            str = String(
                unicodeHandle(code.copyOfRange(offset, offset + len)),
                Charset.forName("Unicode")
            )
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, e.message.toString())
        }
        return str
    }


    /**
     * 从字节数组中解析出Date数据，年月日周时分秒
     */
    @Throws(IndexOutOfBoundsException::class)
    fun bytes2date(data: ByteArray, offset: Int): DateTime {
        var index = offset
        if (data.size < index + 7) {
            throw IndexOutOfBoundsException("length less than 7")
        }
        val sec = data[index++].toInt() // 秒
        val min = data[index++].toInt() // 分
        val hour = data[index++].toInt() // 时
        index++ // 周
        val day = data[index++].toInt() // 日
        val month = data[index++].toInt() // 月
        val year = data[index].toInt() // 年
        return DateTime(year + 2000, month - 1, day, hour, min, sec)
    }

    fun date2bytes(date: DateTime?): ByteArray {
        val data = ByteArray(7)
        date?.let {
            data[0] = it.second.toByte()
            data[1] = it.minute.toByte()
            data[2] = it.hour.toByte()
            data[3] = it.dayOfMonth.toByte()
            data[4] = it.monthNumber.toByte()
            setShort(data, 5, it.year.toShort())
        }
        return data
    }

    fun floatStr2BCD(price: String, integerByte: Int, decimalByte: Int): ByteArray {
        Log.e(TAG, "$price, ${integerByte}, $decimalByte")
        val data = ByteArray(integerByte + decimalByte)
        val split = price.split("\\.").toTypedArray()
        if (split.size > 1 && split[1].length % 2 != 0) {
            split[1] += 0.toString()
        }
        val integer = reverse(fromHexNoSpace(split[0]))
        if (split.size > 1) {
            val decimals = reverse(fromHexNoSpace(split[1]))
            if (decimals.size >= decimalByte) {
                decimals.copyInto(
                    data,
                    0,
                    decimals.size - decimalByte,
                    decimalByte + decimals.size - decimalByte
                )
            } else {
                decimals.copyInto(data, decimalByte - decimals.size, 0, decimals.size)
            }
        }
        if (integer.size >= integerByte) {
            integer.copyInto(
                data,
                2,
                integer.size - integerByte,
                integerByte + integer.size - integerByte
            )
        } else {
            integer.copyInto(data, decimalByte, 0, integer.size)
        }
        Log.e(TAG, "data:${toHex(data)}")
        return data
    }

    /**
     * 判断字符串是否全部是由数字组成
     */
    fun isNumeric(str: String?): Boolean {
        val pattern = Pattern.compile("[0-9]*")
        var result = false
        str?.let {
            result = pattern.matcher(it).matches()
        }
        return result
    }

    /**
     * 将list集合转换成字符串数组
     */
    fun list2Array(list: List<String?>): Array<String?> {
        val arr = arrayOfNulls<String>(list.size)
        for ((i, s) in list.withIndex()) {
            arr[i] = s
        }
        return arr
    }

    /**
     * 先存低位字节的叫小端模式  大端模式则先保存高位字节
     * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在后,高位在前)的顺序。
     *
     * @param i 5--->00 00 00 05    和bytesToInt2（）配套使用
     * @return 大端模式
     */
    fun intToBytes(i: Int): ByteArray {
        val result = ByteArray(4)
        result[0] = (i shr 24 and 0xFF).toByte()
        result[1] = (i shr 16 and 0xFF).toByte()
        result[2] = (i shr 8 and 0xFF).toByte()
        result[3] = (i and 0xFF).toByte()
        return result
    }

    fun isBitString(string: String): Boolean {
        return string.matches(Regex("^[0-1]+$"))
    }

    fun parseBitStringToBitSet(string: String): BitSet {
        if (string.isEmpty()) {
            return BitSet(0)
        }
        if (!isBitString(string)) {
            throw IllegalArgumentException("invalid binary string:$string")
        }
        val bitSet = BitSet(string.length)
        for (i in string.indices) {
            bitSet[i] = string[i] == '1'
        }
        return bitSet
    }

    /**
     * 解析以空格分割的布尔字符串为BitSet类型
     */
    fun parseBoolStringToBitSet(string: String): BitSet {
        val list = string.split(" ")
        val bitSet = BitSet(list.size)
        for (i in list.indices) {
            bitSet[i] = list[i].toBoolean()
        }
        return bitSet
    }

    fun encoderBitSet(bitSet: BitSet): String {
        return bitSet.toBinaryString()
    }

}