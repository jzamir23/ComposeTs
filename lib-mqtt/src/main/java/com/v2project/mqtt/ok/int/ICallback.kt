package com.v2project.mqtt.ok.int

interface ICallBack<T> {
    fun onSuccess(data: T)
    fun onFail(throwable: Throwable)
}

open class ListenerCallBack<T> : ICallBack<T> {

    /**
     * 通过此函数获取原始数据，返回false不执行onSuccess，onFail函数
     */
    var onResponse: ((String) -> Boolean) = { false }

    override fun onSuccess(data: T) {

    }

    override fun onFail(throwable: Throwable) {
    }

}