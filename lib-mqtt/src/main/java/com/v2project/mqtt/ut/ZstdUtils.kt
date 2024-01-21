package com.v2project.mqtt.ut

import com.v2project.mqtt.ok.bean.Zstd

/**
 * Zstd 压缩字符串
 */
object ZstdUtils {

    lateinit var zstd: Zstd

    /**
     * 压缩
     */
    fun compress(str: String, level: Int = 3): ByteArray {
        if (str.isEmpty()) {
            return byteArrayOf()
        }

        try {
            return zstd.compress(str, level)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return byteArrayOf()
    }

    /**
     * 解压缩
     */
    fun decompress(bytes: ByteArray): String {
        if (bytes.isEmpty()) {
            return ""
        }
        val size = zstd.decompressedSize(bytes)
        val decompressBytes = ByteArray(size)
        zstd.decompress(decompressBytes, bytes)
        return zstd.byteArrayToString(decompressBytes)
    }
}