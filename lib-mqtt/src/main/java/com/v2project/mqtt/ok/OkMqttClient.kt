package com.v2project.mqtt.ok

import android.annotation.SuppressLint
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.v2project.mqtt.ok.bean.AutomaticReconnect
import com.v2project.mqtt.ok.bean.RequestBody
import com.v2project.mqtt.ok.int.ICallBack
import com.v2project.mqtt.ok.int.IMqttClient
import com.v2project.mqtt.ok.int.IMqttListener
import com.v2project.mqtt.ut.NettyAndroidFix
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

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
    private val unInitError = Exception("please call connect function first!")
    private val executor = Executors.newSingleThreadExecutor()

    init {
        NettyAndroidFix.applyFix()
    }

    override fun connect(listener: IMqttListener?, iCallBack: ICallBack<MqttClientConfig>?) {
        this.listener = listener
        if (!clientInitialized()) {
            initializeClient()
        }
        when (client.state) {
            MqttClientState.CONNECTED -> {
                iCallBack?.onSuccess(client.config)
                return
            }

            MqttClientState.CONNECTING, MqttClientState.CONNECTING_RECONNECT -> {
                iCallBack?.onFail(IllegalStateException("Connection in progress"))
                return
            }

            else -> {
                connectWithCallback(iCallBack)
            }
        }
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

    @SuppressLint("NewApi")
    private fun connectWithCallback(iCallBack: ICallBack<MqttClientConfig>? = null) {
        try {
            client.connectWith()
                .cleanStart(cleanSession)
                .sessionExpiryInterval(sessionExpiryInterval)//有效期
                .keepAlive(keepAlive)
                .send()
                .whenCompleteAsync({ connAck, throwable ->
                    if (throwable != null) {
                        iCallBack?.onFail(throwable)
                    } else {
                        Log.d(
                            TAG, "会话详情 -> 会话是否存在: ${connAck.isSessionPresent}, " +
                                    "会话有效期: ${connAck.sessionExpiryInterval.orElse(0)}s, " +
                                    "原因码: ${connAck.reasonCode}"
                        )
                        // 检查会话是否恢复
                        if (connAck.isSessionPresent) {
                            Log.i(TAG, "会话恢复成功，过期时间：${connAck.sessionExpiryInterval.orElse(0)}s")
                        } else {
                            Log.i(TAG, "新建会话!")
                        }
                        iCallBack?.onSuccess(client.config)
                    }
                }, executor)
        } catch (e: Exception) {
            iCallBack?.onFail(e)
        }
    }

    @SuppressLint("NewApi")
    override fun subscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>?) {
        if (!clientInitialized() || !client.state.isConnected) {
            iCallBack?.onFail(unInitError)
            return
        }
        val futures = mutableListOf<CompletableFuture<*>>()
        val errors = mutableListOf<Throwable>()
        topics.forEach { topic ->
            try {
                val future = client.subscribeWith()
                    .topicFilter(topic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .noLocal(true)//不接收自己发布的消息
                    .callback { publish ->
                        listener?.onReceiveMessage(client, publish)
                    }
                    .send()

                futures.add(future)
            } catch (e: Exception) {
                errors.add(e)
                Log.e(TAG, "Subscribe error: ${e.message}", e)
            }
        }
        if (errors.isNotEmpty()) {
            iCallBack?.onFail(errors.first())
            return
        }
        CompletableFuture.allOf(*futures.toTypedArray())
            .whenCompleteAsync({ _, throwable ->
                if (throwable != null) {
                    iCallBack?.onFail(throwable)
                } else {
                    iCallBack?.onSuccess(client.config)
                }
            }, executor)
    }

    @SuppressLint("NewApi")
    override fun unSubscribe(topics: List<String>, iCallBack: ICallBack<MqttClientConfig>?) {
        if (!clientInitialized() || !client.state.isConnected) {
            iCallBack?.onFail(unInitError)
            return
        }
        val futures = mutableListOf<CompletableFuture<*>>()
        val errors = mutableListOf<Throwable>()
        topics.forEach { topic ->
            try {
                val future = client.unsubscribeWith()
                    .topicFilter(topic)
                    .send()
                    .thenApply {
                        iCallBack?.onSuccess(client.config)
                    }

                futures.add(future)
            } catch (e: Exception) {
                errors.add(e)
                Log.e(TAG, "Unsubscribe error: ${e.message}", e)
            }
        }

        if (errors.isNotEmpty()) {
            iCallBack?.onFail(errors.first())
        }
    }

    @SuppressLint("NewApi")
    fun publish(body: RequestBody, iCallBack: ICallBack<MqttClientConfig>? = null) {
        if (!clientInitialized() || !client.state.isConnected) {
            iCallBack?.onFail(unInitError)
            return
        }
        try {
            client.publishWith()
                .topic(body.topic)
                .payload(body.payload)
                .qos(body.qos)
                .retain(body.retain)
                .messageExpiryInterval(body.messageExpiryInterval)
                .send()
                .whenCompleteAsync({ _, throwable ->
                    if (throwable != null) {
                        iCallBack?.onFail(throwable)
                    } else {
                        iCallBack?.onSuccess(client.config)
                    }
                }, executor)
        } catch (e: Exception) {
            iCallBack?.onFail(e)
        }
    }

    override fun mqttClientState(): MqttClientState {
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
        private const val TAG = "OkMqttClient"
    }

    class Builder {
        var host: String = ""
        var port: Int = 0
        var identifier: String = ""
        var keepAlive: Int = 30
        var cleanSession: Boolean = false // 默认false 支持离线消息
        var sessionExpiryInterval = 7 * 24 * 3600L // 默认7天会话
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