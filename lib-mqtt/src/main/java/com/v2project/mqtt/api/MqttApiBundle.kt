package com.v2project.mqtt.api

import com.v2project.mqtt.ok.bean.RequestBody
import kotlinx.coroutines.Job

class MqttApiBundle {
    var token = ""                                    // 令牌
    var request = ""                                  // 请求数据
    var response: String? = null                      // 响应数据
    var job: Job? = null                              // 协程上下文
    var status = StatusType.ING                       // 执行状态
    var retry = 0                                     // 重试次数
    var curRetry = 0                                  // 当前重试次数
    var timeout: Long = 60                            // 超时时间（秒）
    var broadcast = false                             // 广播消息
    var body = RequestBody()
    var onComplete: ((MqttApiBundle) -> Unit)? = null // 执行结果

    enum class StatusType {
        ING, SUCCESS, TIME_OUT;

        fun success() = this == SUCCESS
    }
}
