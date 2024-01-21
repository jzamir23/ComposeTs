package com.v2project.mqtt.ut

import com.soywiz.kds.BitSet
import com.v2project.mqtt.ut.Utils.parseBitStringToBitSet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class BitSetAsStringSerializer : KSerializer<BitSet> {
    override val descriptor =
        PrimitiveSerialDescriptor(BitSet::class.simpleName.toString(), PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BitSet) =
        encoder.encodeString(value.toBinaryString())

    override fun deserialize(decoder: Decoder) = parseBitStringToBitSet(decoder.decodeString())
}
