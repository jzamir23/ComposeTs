package com.example.composets

import android.app.Activity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.composets.databinding.ActivityMainBinding
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.v2project.mqtt.api.MqttApi
import com.v2project.mqtt.ok.OkMqttClient
import com.v2project.mqtt.ok.bean.AutomaticReconnect
import com.v2project.mqtt.ok.bean.ConfigRequest
import com.v2project.mqtt.ok.int.ICallBack
import com.v2project.mqtt.ok.int.IMqttListener
import com.v2project.mqtt.ut.CompressUtils
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
    private val deviceId = "66601011423235"
    private val clientId = "test@66601011423235"

    private val api: MqttApi by lazy {
        MqttApi(OkMqttClient.build {
            host = "113.98.58.59"
            port = 13883
            keepAlive = 60
            identifier = clientId
            cleanSession = false
            sessionExpiryInterval = 48 * 3600 // 永不过期：4294967295L
            reconnect = AutomaticReconnect(initialDelay = 2)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.tvSend.setOnClickListener {
            val request = ConfigRequest(recordId = "12123211233", status = 2)
            val option = api.requestOptions
            api.publish(topic = "hv08/upgrade/66601011423235", request = request, options = option,
                object : ICallBack<MqttClientConfig> {
                    override fun onSuccess(data: MqttClientConfig) {
                        logger.debug { "发送消息成功：$request" }
                    }

                    override fun onFail(throwable: Throwable) {
                        logger.error { "发送消息失败：$throwable" }
                    }
                })
        }

        binding.tvConnect.setOnClickListener {
            scope.launch {
                try {
                    logger.debug { "开始MQTT连接..." }
                    api.connect(object : IMqttListener {
                        override fun onReceiveMessage(client: MqttClient, publish: Mqtt5Publish) {
                            val topic = publish.topic.toString()
                            val messageStr = CompressUtils.decompress(bytes = publish.payloadAsBytes)
                            logger.debug { "收到消息 topic: $topic，response: $messageStr" }
                        }

                        override fun onDisconnected(cause: Throwable) {
                            logger.error { "连接异常：$cause" }
                        }

                        override fun onConnected() {
                            logger.debug { "连接成功" }
                        }
                    }, object : ICallBack<MqttClientConfig> {
                        override fun onSuccess(data: MqttClientConfig) {
                            logger.debug { "首次连接成功：${data.serverAddress}，开始订阅..." }
                            val list = listOf("hv08/upgrade/$deviceId")
                            api.subscribe(list, object : ICallBack<MqttClientConfig> {
                                override fun onSuccess(data: MqttClientConfig) {
                                    logger.debug { "订阅成功: $list" }
                                }

                                override fun onFail(throwable: Throwable) {
                                    logger.error { "订阅失败: $throwable" }
                                }
                            })
                        }

                        override fun onFail(throwable: Throwable) {
                            logger.error { "连接失败：$throwable" }
                        }
                    })
                    connectJob?.cancel()
                    connectJob = scope.launch { //订阅成功后
                        while (true) {
                            val status = api.mqttClientState()
                            logger.debug { "MQTT连接状态：${status}" }
                            delay(5 * 1000)// 间隔5秒检查一次
                        }
                    }
                } catch (e: Exception) {
                    logger.error { "其他异常：" + e.message }
                }
            }
        }
    }
}
