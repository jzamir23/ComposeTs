package com.v2project.mqtt.ok.bean

import java.util.concurrent.TimeUnit

/**
 * 客户端自动连接参数
 */
data class AutomaticReconnect(
    var initialDelay: Long = 1,                             // 初始延迟
    var initialDelayUnit: TimeUnit = TimeUnit.SECONDS,      // 初始延迟（单位）
    var maxDelay: Long = 30,                                // 最大延迟
    var maxDelayUnit: TimeUnit = TimeUnit.SECONDS           // 最大延迟（单位）
)