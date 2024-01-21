package com.v2project.mqtt.ok.int

import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientState

interface IMqttClient {
    fun connect(iCallBack: ICallBack<MqttClientConfig>? = null)
    fun subscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>? = null)
    fun unSubscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>? = null)
    fun getMqttClientState(): MqttClientState
    fun disconnect()
}
