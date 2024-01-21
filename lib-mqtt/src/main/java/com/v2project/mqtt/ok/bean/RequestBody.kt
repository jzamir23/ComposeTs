package com.v2project.mqtt.ok.bean

import com.hivemq.client.mqtt.datatypes.MqttQos

class RequestBody {
    var payload = byteArrayOf()                   // 消息载体
    var topic = ""                                // 主题
    var qos = MqttQos.AT_LEAST_ONCE               // 服务质量（默认AT_LEAST_ONCE：确保至少一次交付）
    var retain = false                            // 保留发布消息（默认false：不保留）
}
