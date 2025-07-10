package com.v2project.mqtt.ok.int

import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientState

interface IMqttClient {
    fun connect(listener: IMqttListener?, iCallBack: ICallBack<MqttClientConfig>? = null)
    fun subscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>? = null)
    fun unSubscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>? = null)
    fun mqttClientState(): MqttClientState
    fun disconnect()
}