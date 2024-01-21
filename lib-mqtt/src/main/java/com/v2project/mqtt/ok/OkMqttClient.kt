package com.v2project.mqtt.ok

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.v2project.mqtt.ok.bean.AutomaticReconnect
import com.v2project.mqtt.ok.bean.RequestBody
import com.v2project.mqtt.ok.int.ICallBack
import com.v2project.mqtt.ok.int.IMqttClient
import com.v2project.mqtt.ok.int.IMqttListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class OkMqttClient private constructor(
    private var host: String,
    private var port: Int,
    private var identifier: String,
    private var keepAlive: Int,
    private var cleanSession: Boolean,
    private var connectDelay: Long,
    private var connectDelayUnit: TimeUnit,
    private var reconnect: AutomaticReconnect
) : IMqttClient {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var client: Mqtt3AsyncClient
    var listener: IMqttListener? = null
    private val unInitError = Exception("please call connect function first !!!")

    override fun connect(iCallBack: ICallBack<MqttClientConfig>?) {
        if (!clientInitialized()) {
            val builder =
                MqttClient.builder().useMqttVersion3().identifier(identifier).serverHost(host)
                    .serverPort(port)
            reconnect.let {
                builder.automaticReconnect().initialDelay(it.initialDelay, it.initialDelayUnit)
                    .maxDelay(it.maxDelay, it.maxDelayUnit).applyAutomaticReconnect()
            }
            client = builder.addDisconnectedListener { iCallBack?.onFail(it.cause) }.buildAsync()

            connectAck(object : ICallBack<MqttClientConfig> {
                override fun onSuccess(data: MqttClientConfig) {
                    iCallBack?.onSuccess(client.config)
                }

                override fun onFail(throwable: Throwable) {
                    if (connectDelay != 0L) {
                        scope.launch {
                            delay(connectDelayUnit.toMillis(connectDelay))
                            connectAck()
                        }
                    }
                }
            })
        } else {
            iCallBack?.onSuccess(client.config)
        }
    }

    private fun connectAck(iCallBack: ICallBack<MqttClientConfig>? = null) {
        val throwable = try {
            client.connectWith().keepAlive(keepAlive).cleanSession(cleanSession).send()
            null
        } catch (e: Exception) {
            e
        }
        if (throwable == null) {
            iCallBack?.onSuccess(client.config)
        } else {
            iCallBack?.onFail(throwable)
        }
    }

    private val consumer: Consumer<Mqtt3Publish> = Consumer<Mqtt3Publish> { t ->
        listener?.onReceiveMessage(client, t)
    }

    override fun subscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>?) {
        if (clientInitialized()) {
            var t: Throwable? = null
            topics.forEach {
                try {
                    client.subscribeWith().topicFilter(it).callback(consumer).send()
                } catch (e: Exception) {
                    t = e
                }
            }
            t?.let {
                iCallBack?.onFail(it)
            } ?: kotlin.run {
                iCallBack?.onSuccess(client.config)
            }
        } else {
            iCallBack?.onFail(unInitError)
        }
    }

    override fun unSubscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>?) {
        if (clientInitialized()) {
            topics.forEach {
                try {
                    client.unsubscribeWith().topicFilter(it).send()
                    iCallBack?.onSuccess(client.config)
                } catch (e: Exception) {
                    iCallBack?.onFail(e)
                }
            }
        } else {
            iCallBack?.onFail(unInitError)
        }
    }

    fun publish(body: RequestBody, iCallBack: ICallBack<MqttClientConfig>? = null) {
        if (clientInitialized()) {
            try {
                client.publishWith().topic(body.topic).payload(body.payload).qos(body.qos)
                    .retain(body.retain).send()
                iCallBack?.onSuccess(client.config)
            } catch (e: Exception) {
                iCallBack?.onFail(e)
            }
        } else {
            iCallBack?.onFail(unInitError)
        }
    }

    override fun getMqttClientState() =
        if (clientInitialized()) client.state else MqttClientState.DISCONNECTED

    override fun disconnect() {
        if (clientInitialized()) {
            client.disconnect()
        }
    }

    private fun clientInitialized(): Boolean = this::client.isInitialized

    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        var host: String = ""
        var port: Int = 0
        var identifier: String = ""
        var keepAlive: Int = 30
        var cleanSession: Boolean = true
        var connectDelay: Long = 2
        var connectDelayUnit: TimeUnit = TimeUnit.SECONDS
        var reconnect: AutomaticReconnect = AutomaticReconnect()

        fun build() =
            OkMqttClient(
                host,
                port,
                identifier,
                keepAlive,
                cleanSession,
                connectDelay,
                connectDelayUnit,
                reconnect
            )
    }
}

