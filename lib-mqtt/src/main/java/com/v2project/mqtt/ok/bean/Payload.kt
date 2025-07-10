package com.v2project.mqtt.ok.bean

import com.v2project.mqtt.ut.nowDateTime
import com.v2project.mqtt.ut.toHex
import kotlinx.serialization.Serializable
import kotlin.random.Random

interface Payload

@Serializable
class DefaultPayload : Payload

@Suppress("PropertyName")
@Serializable
open class ResponsePayload : Payload {
    var RT_F: Int = EnumRT.SUCCESS.code
    var RT_D: String = EnumRT.SUCCESS.desc

    override fun toString(): String {
        return "ResponseResult(RT_F=$RT_F, RT_D=$RT_D)"
    }

    fun success() = this.RT_F == EnumRT.SUCCESS.code
}

/**
 * 传输 Mqtt Payload
 */
@Serializable
data class TransportPayload(
    var token: String = "${nowDateTime()}*${Random.nextInt()}",
    var timestamp: String = nowDateTime().toString(),
    var compress: Int = EnumCompress.ZSTD.ordinal,
    var encryption: Int = 0,
    var payloadBytes: ByteArray = ByteArray(0)
) : Payload {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransportPayload

        if (token != other.token) return false
        if (timestamp != other.timestamp) return false
        if (compress != other.compress) return false
        if (encryption != other.encryption) return false
        if (!payloadBytes.contentEquals(other.payloadBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = token.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + compress.hashCode()
        result = 31 * result + encryption.hashCode()
        result = 31 * result + payloadBytes.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "TransportPayload(token='$token', " +
                "timestamp='$timestamp', " +
                "compress=${valueOfCompress(compress)}, " +
                "encryption=${valueOfEncrypt(encryption)}, " +
                "payloadBytes=${payloadBytes.toHex()})"
    }
}

/**
 * 压缩方式
 */
enum class EnumCompress(val desc: String) {
    UNCOMPRESSED("未压缩"),
    LZMA("LZMA压缩"),
    DEFLATER("DEFLATE压缩"),
    ZSTD("ZSTD压缩"),
    GZIP("GZIP压缩"),
    UNKNOWN("未知方式");
}

fun valueOfCompress(ordinal: Int) = EnumCompress.values().find { it.ordinal == ordinal } ?: EnumCompress.UNKNOWN

/**
 * 加密方式
 */
enum class EnumEncrypt(val desc: String) {
    UNENCRYPTED("未加密"),
    ESAM("ESAM加密"),
    DES("DES加密"),
    AES("AES加密"),
    RSA("RSA加密"),
    UNKNOWN("未知加密方式"),
}

fun valueOfEncrypt(ordinal: Int): EnumEncrypt {
    return EnumEncrypt.values().find { it.ordinal == ordinal } ?: EnumEncrypt.UNKNOWN
}

enum class EnumRT(val code: Int, val desc: String) {
    SUCCESS(0, "任务执行成功"),
    ArgsERR(1, "参数错误:请检查参数是否缺失或是参数名称错误"),
    DataErr(2, "数据错误:"),
    ExecErr(3, "任务执行失败"),
    ExecFail(4, "任务执行完成，结果失败"),
    InnerNetFail(100, "专网网络不通"),
    InternetFail(101, "调试网网络不通"),
    UNKNOWN(0xFFFFFFFF.toInt(), "未知错误"),
}

fun valueOfRT(ordinal: Int): EnumRT {
    return EnumRT.values().find { it.ordinal == ordinal } ?: EnumRT.UNKNOWN
}

@Serializable
data class ConfigRequest(
    var recordId: String = "",              //下发任务时参数，android通知后台要带上这个
    var deviceType: String = "HV06",        //HV06，HV05
    var status: Int = -1,                   //-1失败，1已完成，2拒绝更新
) : Payload