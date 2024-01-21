package com.v2project.mqtt.ut

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class DateTimeAsStringSerializer(private val format: String = "yyyy-MM-dd HH:mm:ss.SSS") :
    KSerializer<DateTime> {

    override val descriptor =
        PrimitiveSerialDescriptor(DateTime::class.simpleName.toString(), PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DateTime) =
        encoder.encodeString(date2String(value, format))

    override fun deserialize(decoder: Decoder) = string2Date(decoder.decodeString(), format)
}