package com.v2project.mqtt.ut

import com.v2project.mqtt.ok.bean.EnumCompress
import com.v2project.mqtt.ok.bean.Zstd

object CompressUtils {
    init {
        ZstdUtils.zstd = Zstd()
    }

    fun compress(compress: Int, json: String, level: Int = 15): ByteArray {
        return when (compress) {
            EnumCompress.UNCOMPRESSED.ordinal -> {
                json.toByteArray()
            }

            EnumCompress.ZSTD.ordinal -> {
                ZstdUtils.compress(json, level)
            }

            else -> {
                DeflaterUtils.compress(json)
            }
        }
    }

    fun decompress(compress: Int = EnumCompress.UNCOMPRESSED.ordinal, bytes: ByteArray): String {
        return when (compress) {
            EnumCompress.UNCOMPRESSED.ordinal -> {
                String(bytes)
            }

            EnumCompress.ZSTD.ordinal -> {
                ZstdUtils.decompress(bytes)
            }

            else -> {
                DeflaterUtils.decompress(bytes)
            }
        }
    }
}