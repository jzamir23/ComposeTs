package com.v2project.mqtt.api

import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientState
import com.v2project.mqtt.ok.OkMqttClient
import com.v2project.mqtt.ok.bean.EnumRT
import com.v2project.mqtt.ok.bean.Payload
import com.v2project.mqtt.ok.bean.ResponsePayload
import com.v2project.mqtt.ok.bean.TransportPayload
import com.v2project.mqtt.ok.int.ICallBack
import com.v2project.mqtt.ok.int.IMqttClient
import com.v2project.mqtt.ok.int.IMqttListener
import com.v2project.mqtt.ok.int.ListenerCallBack
import com.v2project.mqtt.ut.CompressUtils
import com.v2project.mqtt.ut.JsonLazyFormat
import com.v2project.mqtt.ut.decodeFromString
import com.v2project.mqtt.ut.encodeToString
import kotlin.reflect.full.createInstance

class MqttApi(var mqttClient: OkMqttClient) : IMqttClient {
    val format by lazy { JsonLazyFormat.lazyFormat(encodeDefaults = true) }
    val mqttApiPool = MqttApiPool()
    val requestOptions by lazy { RequestOptions() }

    /*init {
        mqttClient.listener = object : IMqttListener {
            override fun onReceiveMessage(client: MqttClient, publish: Mqtt5Publish) {
                val payloadStr = String(publish.payloadAsBytes)
                decodeFromString<TransportPayload>(format, payloadStr)?.let {
                    mqttApiPool.match(
                        publish.topic.toString(),
                        it.token,
                        CompressUtils.decompress(it.compress, it.payloadBytes)
                    )
                } ?: run {
                    mqttApiPool.listenerList.forEach {
                        it.onUnknown(MqttApiBundle().apply {
                            this.body.topic = publish.topic.toString()
                            this.response = payloadStr
                        })
                    }
                }
            }

            override fun onDisconnected(cause: Throwable) {

            }

            override fun onConnected() {

            }
        }
    }*/

    override fun connect(listener: IMqttListener?, iCallBack: ICallBack<MqttClientConfig>?) {
        mqttClient.connect(listener, iCallBack)
    }

    override fun subscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>?) {
        mqttClient.subscribe(topics, iCallBack)
    }

    override fun unSubscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>?) {
        mqttClient.unSubscribe(topics, iCallBack)
    }

    override fun getMqttClientState(): MqttClientState {
        return mqttClient.getMqttClientState()
    }

    override fun disconnect() {
        mqttClient.disconnect()
        mqttApiPool.reset()
    }

    inline fun <reified Request : Payload, reified Response : ResponsePayload> publish(
        topic: String,
        request: Request? = null,
        options: RequestOptions? = null,
        iCallBack: ICallBack<Response>
    ) {
        val bundle = MqttApiBundle().apply {
            val requestStr = request?.let { encodeToString(format, it) } ?: run { "" }
            val mOptions = options ?: requestOptions
            val transportPayload = TransportPayload().also {
                it.payloadBytes = CompressUtils.compress(mOptions.compress, requestStr)
                it.compress = mOptions.compress
            }
            this.body.apply {
                this.topic = topic
                //this.payload = encodeToString(format, transportPayload).toByteArray()
                this.payload = requestStr.toByteArray()
                this.qos = mOptions.qos
                this.retain = mOptions.retain
            }
            this.token = transportPayload.token
            this.retry = mOptions.retry
            this.timeout = mOptions.timeout
            this.broadcast = mOptions.broadcast
            this.request = requestStr

            this.onComplete = { bundle ->
                if (bundle.status.success()) {
                    bundle.response?.let {
                        if (iCallBack is ListenerCallBack) {
                            if (iCallBack.onResponse(it)) {
                                decodeResponse(it, iCallBack)
                            }
                        } else {
                            decodeResponse(it, iCallBack)
                        }
                    } ?: run {
                        iCallBack.onSuccess(Response::class.createInstance())
                    }
                } else {
                    iCallBack.onFail(TimeoutException("$topic $token timeout"))
                }
            }
        }
        mqttApiPool.add(bundle) { mqttClient.publish(bundle.body) }
    }

    inline fun <reified Response : ResponsePayload> decodeResponse(
        response: String,
        iCallBack: ICallBack<Response>
    ) {
        decodeFromString<Response>(format, response)?.let { res ->
            if (res.success()) {
                iCallBack.onSuccess(res)
            } else {
                iCallBack.onFail(ApiException(res.RT_F, res.RT_D))
            }
        } ?: run {
            iCallBack.onFail(ApiException(EnumRT.UNKNOWN.code, "decodeResponse fail：$response"))
        }
    }

    fun addMqttApiListener(listener: IMqttApiListener) {
        mqttApiPool.addListener(listener)
    }

    fun removeMqttApiListener(listener: IMqttApiListener) {
        mqttApiPool.removeListener(listener)
    }
}


