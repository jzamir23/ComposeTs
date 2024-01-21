package com.v2project.mqtt.ok.bean

import com.github.luben.zstd.Zstd

class Zstd {
    fun compress(str: String, level: Int): ByteArray {
        return Zstd.compress(str.toByteArray(), level)
    }

    fun decompress(dst: ByteArray, src: ByteArray) {
        Zstd.decompress(dst, src)
    }

    fun decompressedSize(bytes: ByteArray): Int {
        return Zstd.decompressedSize(bytes).toInt()
    }

    fun byteArrayToString(bytes: ByteArray): String {
        return String(bytes)
    }
}