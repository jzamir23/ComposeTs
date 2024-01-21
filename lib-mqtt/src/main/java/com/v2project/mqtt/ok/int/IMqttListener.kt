package com.v2project.mqtt.ok.int

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish

interface IMqttListener {
    fun onReceiveMessage(client: MqttClient, publish: Mqtt3Publish)
}
