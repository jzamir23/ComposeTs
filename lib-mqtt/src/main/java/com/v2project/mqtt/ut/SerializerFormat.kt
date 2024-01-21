package com.v2project.mqtt.ut

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> encodeToString(json: Json, value: T): String = json.encodeToString(value)

inline fun <reified T> decodeFromString(json: Json, str: String): T? =
    try {
        json.decodeFromString<T>(str)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }