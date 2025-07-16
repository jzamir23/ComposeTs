package com.v2project.mqtt.ut

import android.util.Log

object NettyAndroidFix {
    private var isFixed = false

    fun applyFix() {
        if (isFixed) return

        try {
            // 设置 Netty 属性
            System.setProperty("io.netty.logging", "SLF4J")
            System.setProperty("io.netty.noPreferDirect", "true")
            System.setProperty("io.netty.noUnsafe", "true")
            System.setProperty("io.netty.transport.noNative", "true")
            // 使用合法的全零 MAC 地址格式
            System.setProperty("io.netty.machineId", "00:00:00:00:00:00")
            System.setProperty("io.netty.processId", "" + android.os.Process.myPid())

            // 强制初始化关键 Netty 类
            Class.forName("io.netty.util.internal.SystemPropertyUtil")
            Class.forName("io.netty.util.NetUtil")
            Class.forName("io.netty.channel.nio.NioEventLoopGroup")

            isFixed = true
        } catch (e: Exception) {
            Log.e("NettyFix", "Failed to apply Netty fix", e)
        }
    }
}