package com.v2project.mqtt.ok.int

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish

interface IMqttListener {
    fun onReceiveMessage(client: MqttClient, publish: Mqtt5Publish)
    fun onConnected() // 连接成功回调
    fun onDisconnected(cause: Throwable)
}
