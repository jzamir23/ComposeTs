package com.v2project.mqtt.api

import com.v2project.mqtt.ok.int.ICallBack
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * MqttApiPool
 */
class MqttApiPool {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val bundleList by lazy { mutableListOf<MqttApiBundle>() }
    val listenerList by lazy { mutableListOf<IMqttApiListener>() }
    private val mutex = Mutex()

    fun add(bundle: MqttApiBundle, run: (suspend () -> Unit)) {
        scope.launch {
            mutex.withLock {
                repeat(bundle, run)
                bundleList.add(bundle)
                listenerList.forEach { listener ->
                    listener.onAdd(bundle)
                }
            }
        }
    }

    private fun repeat(bundle: MqttApiBundle, run: suspend () -> Unit) {
        bundle.job = scope.launch {
            launch { run() }
            delay(bundle.timeout)
            if (bundle.retry > 0 && bundle.curRetry < bundle.retry) {
                bundle.curRetry = bundle.curRetry + 1
                listenerList.forEach { listener ->
                    listener.onRepeat(bundle)
                }
                repeat(bundle, run)
                return@launch
            }
            bundle.status = MqttApiBundle.StatusType.TIME_OUT
            listenerList.forEach { listener ->
                listener.onTimeout(bundle)
            }
            remove(bundle.token)
        }
    }

    fun remove(token: String) {
        scope.launch {
            mutex.withLock {
                bundleList.firstOrNull {
                    it.token == token
                }?.let {
                    it.job?.cancel()
                    it.onComplete?.invoke(it)
                    bundleList.remove(it)
                    listenerList.forEach { listener ->
                        listener.onRemove(it)
                    }
                }
            }
        }
    }

    fun match(topic: String, token: String, response: Any, callback: ICallBack<MqttApiBundle>? = null) {
        scope.launch {
            mutex.withLock {
                bundleList.firstOrNull {
                    it.token == token
                }?.let {
                    it.status = MqttApiBundle.StatusType.SUCCESS
                    it.response = response as String
                    callback?.onSuccess(it)
                    if (it.broadcast) {
                        it.onComplete?.invoke(it)
                    } else {
                        remove(token)
                        listenerList.forEach { listener ->
                            listener.onComplete(it)
                        }
                    }
                } ?: run {
                    callback?.onFail(Exception("$topic $token not match"))
                    listenerList.forEach { listener ->
                        listener.onMatchFail(MqttApiBundle().apply {
                            this.token = token
                            this.body.topic = topic
                            this.response = response as String
                        })
                    }
                }
            }
        }
    }

    fun addListener(listener: IMqttApiListener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener)
        }
    }

    fun removeListener(listener: IMqttApiListener) {
        listenerList.remove(listener)
    }

    fun reset() {
        bundleList.clear()
    }
}