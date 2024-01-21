package com.v2project.mqtt.ut

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object JsonLazyFormat {
    fun lazyFormat(
        format: String = "yyyy-MM-dd HH:mm:ss.SSS",
        prettyPrint: Boolean = true,
        encodeDefaults: Boolean = false
    ) = Json {
        this.prettyPrint = prettyPrint
        this.encodeDefaults = encodeDefaults
        this.ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(DateTimeAsStringSerializer(format))
            contextual(BitSetAsStringSerializer())
        }
    }
}