package com.v2project.mqtt.api

class ApiException(var code: Int, override var message: String) : Exception(message)

class TimeoutException(override var message: String = "timeout") : Exception(message)