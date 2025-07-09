package com.example.composets

import android.app.Activity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.composets.databinding.ActivityMainBinding
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.v2project.mqtt.api.ApiException
import com.v2project.mqtt.api.MqttApi
import com.v2project.mqtt.api.TimeoutException
import com.v2project.mqtt.api.connectAsync
import com.v2project.mqtt.api.subscribeAsync
import com.v2project.mqtt.ok.OkMqttClient
import com.v2project.mqtt.ok.bean.AutomaticReconnect
import com.v2project.mqtt.ok.int.IMqttListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

class MainActivity : Activity() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val logger = KotlinLogging.logger { }
    private var connectJob: Job? = null
    private val deviceId = "6660101142323"
    private val clientId = "test@6660101142323"
    private val client = OkMqttClient.build {
        host = "113.98.58.59"
        port = 13883
        keepAlive = 35
        identifier = clientId
        cleanSession = false
        sessionExpiryInterval = 6 * 3600L
        reconnect = AutomaticReconnect(initialDelay = 3)
    }

    private val api: MqttApi by lazy { MqttApi(client) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.tvDisconnect.setOnClickListener {
            api.disconnect()
        }

        scope.launch {
            try {
                logger.error { "开始MQTT连接..." }
                api.connectAsync(object : IMqttListener {
                    override fun onReceiveMessage(client: MqttClient, publish: Mqtt5Publish) {
                        val topic = publish.topic.toString()
                        val messageStr = String(publish.payloadAsBytes)
                        logger.debug { "收到消息，topic: $topic，response: $messageStr" }
                    }

                    override fun onDisconnected(cause: Throwable) {
                        logger.error { "连接异常：$cause" }
                    }

                    override fun onConnected() {
                        logger.debug { "连接成功，当前状态：${api.getMqttClientState()}" }
                        scope.launch {
                            val list = listOf("hv08/upgrade/$deviceId")
                            api.subscribeAsync(list)
                            logger.error { "订阅成功: $list" }
                        }
                    }
                })
                connectJob?.cancel()
                connectJob = scope.launch { //订阅成功后
                    while (true) {
                        val status = api.getMqttClientState()
                        logger.debug { "MQTT连接状态：${status}" }
                        delay(5 * 1000)// 间隔5秒检查一次
                    }
                }
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
}
