package com.v2project.mqtt.api

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.v2project.mqtt.ok.bean.EnumCompress

class RequestOptions {
    var qos = MqttQos.AT_LEAST_ONCE              // 服务质量（默认AT_LEAST_ONCE：确保至少一次交付）
    var retain = false                           // 保留发布消息（默认false：不保留）
    var retry = 0                                // 重试次数（默认0：不重试）
    var timeout = 60L                            // 超时时间（秒）
    var messageExpiryInterval = 3600L            // 消息过期时间（秒）
    var compress = EnumCompress.UNCOMPRESSED.ordinal  // 不压缩
    var broadcast = false                        // 广播消息（匹配后不移除，直到超时）
}