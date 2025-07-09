package com.v2project.mqtt.ok

import android.os.Build
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.v2project.mqtt.ok.bean.AutomaticReconnect
import com.v2project.mqtt.ok.bean.RequestBody
import com.v2project.mqtt.ok.int.ICallBack
import com.v2project.mqtt.ok.int.IMqttClient
import com.v2project.mqtt.ok.int.IMqttListener
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class OkMqttClient private constructor(
    private var host: String,
    private var port: Int,
    private var identifier: String,
    private var keepAlive: Int,
    private var cleanSession: Boolean,
    private var sessionExpiryInterval: Long,
    private var reconnect: AutomaticReconnect
) : IMqttClient {
    private lateinit var client: Mqtt5AsyncClient
    private var listener: IMqttListener? = null
    private val unInitError = Exception("please call connect function first !!!")

    override fun connect(listener: IMqttListener?, iCallBack: ICallBack<MqttClientConfig>?) {
        this.listener = listener
        if (!clientInitialized()) {
            initializeClient()
        }
        // 如果已连接或正在连接，直接返回成功
        if (client.state.isConnected || client.state.isConnectedOrReconnect) {
            iCallBack?.onSuccess(client.config)
            return
        }

        connectWithCallback(iCallBack)
    }

    private fun initializeClient() {
        val builder = MqttClient.builder()
            .useMqttVersion5()
            .identifier(identifier)
            .serverHost(host)
            .serverPort(port)

        reconnect.let {
            builder.automaticReconnect()
                .initialDelay(it.initialDelay, it.initialDelayUnit)
                .maxDelay(it.maxDelay, it.maxDelayUnit)
                .applyAutomaticReconnect()
        }

        client = builder
            .addConnectedListener {
                listener?.onConnected()
            }.addDisconnectedListener { context ->
                listener?.onDisconnected(context.cause)
            }.buildAsync()
    }

    private fun connectWithCallback(iCallBack: ICallBack<MqttClientConfig>? = null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                client.connectWith()
                    .cleanStart(cleanSession)
                    .sessionExpiryInterval(sessionExpiryInterval)//有效期
                    .keepAlive(keepAlive)
                    .send()
                    .whenComplete { _: Mqtt5ConnAck?, throwable: Throwable? ->
                        if (throwable != null) {
                            iCallBack?.onFail(throwable)
                        } else {
                            iCallBack?.onSuccess(client.config)
                        }
                    }
            } else {
                client.connectWith()
                    .cleanStart(cleanSession)
                    .sessionExpiryInterval(sessionExpiryInterval)
                    .keepAlive(keepAlive)
                    .send()
                iCallBack?.onSuccess(client.config)
            }
        } catch (e: Exception) {
            iCallBack?.onFail(e)
        }
    }

    private val consumer: Consumer<Mqtt5Publish> = Consumer<Mqtt5Publish> { t ->
        listener?.onReceiveMessage(client, t)
    }

    override fun subscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>?) {
        if (clientInitialized()) {
            if (!client.state.isConnected) {
                iCallBack?.onFail(IllegalStateException("Not connected"))
                return
            }
            var lastError: Throwable? = null
            topics.forEach {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        client.subscribeWith()
                            .topicFilter(it)
                            .qos(MqttQos.AT_LEAST_ONCE)
                            .callback(consumer)
                            .send()
                            .get(5, TimeUnit.SECONDS)//5秒超时
                    } else {
                        client.subscribeWith()
                            .topicFilter(it)
                            .qos(MqttQos.AT_LEAST_ONCE)
                            .callback(consumer)
                            .send()
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
            if (lastError == null) {
                iCallBack?.onSuccess(client.config)
            } else {
                iCallBack?.onFail(lastError!!)
            }
        } else {
            iCallBack?.onFail(unInitError)
        }
    }

    override fun unSubscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>?) {
        if (clientInitialized()) {
            if (!client.state.isConnected) {
                iCallBack?.onFail(IllegalStateException("Not connected"))
                return
            }
            topics.forEach {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        client.unsubscribeWith()
                            .topicFilter(it)
                            .send()
                            .get(5, TimeUnit.SECONDS)
                    } else {
                        client.unsubscribeWith()
                            .topicFilter(it)
                            .send()
                    }
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
            if (!client.state.isConnected) {
                iCallBack?.onFail(IllegalStateException("Not connected"))
                return
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    client.publishWith()
                        .topic(body.topic)
                        .payload(body.payload)
                        .qos(body.qos)
                        .retain(body.retain)
                        .messageExpiryInterval(3600)//1小时后过期
                        .send()
                        .whenComplete { _, throwable ->
                            if (throwable != null) {
                                iCallBack?.onFail(throwable)
                            } else {
                                iCallBack?.onSuccess(client.config)
                            }
                        }
                } else {
                    client.publishWith()
                        .topic(body.topic)
                        .payload(body.payload)
                        .qos(body.qos)
                        .retain(body.retain)
                        .messageExpiryInterval(3600)//1小时后过期
                        .send()
                    iCallBack?.onSuccess(client.config)
                }
            } catch (e: Exception) {
                iCallBack?.onFail(e)
            }
        } else {
            iCallBack?.onFail(unInitError)
        }
    }

    override fun getMqttClientState(): MqttClientState {
        return if (clientInitialized()) client.state else MqttClientState.DISCONNECTED
    }

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
        var sessionExpiryInterval = 60 * 60L
        var reconnect: AutomaticReconnect = AutomaticReconnect()

        fun build() = OkMqttClient(
            host,
            port,
            identifier,
            keepAlive,
            cleanSession,
            sessionExpiryInterval,
            reconnect
        )
    }
}

