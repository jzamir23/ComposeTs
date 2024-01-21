package com.example.composets

import android.app.Application
import com.v2project.mqtt.api.ApiException
import com.v2project.mqtt.api.MqttApi
import com.v2project.mqtt.api.TimeoutException
import com.v2project.mqtt.api.connectAsync
import com.v2project.mqtt.api.subscribeAsync
import com.v2project.mqtt.ok.OkMqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.*

class App : Application() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val logger = KotlinLogging.logger { }

    init {
        app = this
    }

    val deviceId = "666010114"
    val clientId = "test@" + UUID.randomUUID().toString()
    val client = OkMqttClient.build {
        this.host = "v2network.cn"
        this.port = 1883
        this.identifier = clientId
    }

    val api: MqttApi by lazy { MqttApi(client) }

    override fun onCreate() {
        super.onCreate()
        api.deviceId = deviceId
        scope.launch {
            try {
                logger.error { "当前状态：${api.getMqttClientState()}" }
                api.connectAsync()
                logger.error { "连接成功，当前状态：${api.getMqttClientState()}" }

                api.subscribeAsync(listOf("Topics$deviceId"))
                logger.error { "订阅成功" }
            } catch (e: Exception) {
                when (e) {
                    is TimeoutException -> {
                        logger.error { "超时异常：${e.message}" }
                    }
                    is ApiException -> {
                        logger.error { "Api异常：${e.code} - ${e.message}" }
                    }
                    else -> {
                        logger.error { "其他异常：" + e.message }
                    }
                }
            }
        }
    }

    companion object {
        lateinit var app: App
    }
}