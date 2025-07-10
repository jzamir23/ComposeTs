package com.v2project.mqtt.ut

import android.util.Log

object NettyAndroidFix {
    private var isFixed = false

    fun applyFix() {
        if (isFixed) return

        try {
//            // 设置 Netty 属性
//            System.setProperty("io.netty.processId", "0")
//            System.setProperty("io.netty.machineId", "0")
//
//            // 确保 Netty 使用 Android 兼容的 DNS 解析
//            System.setProperty("io.netty.handler.ssl.noOpenSsl", "true")
//            System.setProperty("io.netty.transport.noNative", "true")


            // 使用合法的全零 MAC 地址格式
            System.setProperty("io.netty.machineId", "00:00:00:00:00:00")
            System.setProperty("io.netty.processId", "0")

            // 绕过 MAC 地址验证
//            val macAddressUtil = Class.forName("io.netty.util.internal.MacAddressUtil")
//            val bestAvailableMacMethod = macAddressUtil.getDeclaredMethod("bestAvailableMac")
//            bestAvailableMacMethod.isAccessible = true
//            val mac = bestAvailableMacMethod.invoke(null) as ByteArray

//            // 设置有效的机器ID
//            val defaultChannelId = Class.forName("io.netty.channel.DefaultChannelId")
//            val machineIdField = defaultChannelId.getDeclaredField("machineId")
//            machineIdField.isAccessible = true
//            machineIdField.set(null, mac)

//            // 初始化 Netty 避免后续错误
//            val defaultProcessIdMethod = defaultChannelId.getDeclaredMethod("defaultProcessId")
//            defaultProcessIdMethod.isAccessible = true
//            defaultProcessIdMethod.invoke(null)

            isFixed = true
        } catch (e: Exception) {
            Log.e("NettyFix", "Failed to apply Netty fix", e)
        }
    }
}