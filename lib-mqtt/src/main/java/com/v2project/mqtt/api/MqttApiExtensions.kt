package com.v2project.mqtt.api

import com.v2project.mqtt.ok.bean.Payload
import com.v2project.mqtt.ok.bean.ResponsePayload
import com.hivemq.client.mqtt.MqttClientConfig
import com.v2project.mqtt.ok.int.ICallBack
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend inline fun <reified Request : Payload, reified Response : ResponsePayload> MqttApi.publishAsync(
    topic: String,
    request: Request?,
    options: RequestOptions? = null
): Response = suspendCoroutine { continuation ->
    publish(topic, request, options, object : ICallBack<Response> {
        override fun onSuccess(data: Response) {
            continuation.resume(value = data)
        }

        override fun onFail(throwable: Throwable) {
            continuation.resumeWithException(throwable)
        }
    })
}

suspend fun MqttApi.connectAsync(): MqttClientConfig = suspendCoroutine { continuation ->
    connect(object : ICallBack<MqttClientConfig> {
        override fun onSuccess(data: MqttClientConfig) {
            continuation.resume(value = data)
        }

        override fun onFail(throwable: Throwable) {
            continuation.resumeWithException(throwable)
        }
    })
}

suspend fun MqttApi.subscribeAsync(topics: List<String>): MqttClientConfig = suspendCoroutine { continuation ->
    subscribe(topics, object : ICallBack<MqttClientConfig> {
        override fun onSuccess(data: MqttClientConfig) {
            continuation.resume(value = data)
        }

        override fun onFail(throwable: Throwable) {
            continuation.resumeWithException(throwable)
        }
    })
}

suspend fun MqttApi.unSubscribeAsync(topics: List<String>): MqttClientConfig = suspendCoroutine { continuation ->
    unSubscribe(topics, object : ICallBack<MqttClientConfig> {
        override fun onSuccess(data: MqttClientConfig) {
            continuation.resume(value = data)
        }

        override fun onFail(throwable: Throwable) {
            continuation.resumeWithException(throwable)
        }
    })
}

