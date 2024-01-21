package com.v2project.mqtt.api

interface IMqttApiListener {
    fun onAdd(bundle: MqttApiBundle)
    fun onRepeat(bundle: MqttApiBundle)
    fun onComplete(bundle: MqttApiBundle)
    fun onTimeout(bundle: MqttApiBundle)
    fun onRemove(bundle: MqttApiBundle)
    fun onMatchFail(bundle: MqttApiBundle)
    fun onUnknown(bundle: MqttApiBundle)
}

open class MqttApiListener : IMqttApiListener {
    override fun onAdd(bundle: MqttApiBundle) {
    }

    override fun onRepeat(bundle: MqttApiBundle) {
    }

    override fun onComplete(bundle: MqttApiBundle) {
    }

    override fun onTimeout(bundle: MqttApiBundle) {
    }

    override fun onRemove(bundle: MqttApiBundle) {
    }

    override fun onMatchFail(bundle: MqttApiBundle) {
    }

    override fun onUnknown(bundle: MqttApiBundle) {
    }
}
