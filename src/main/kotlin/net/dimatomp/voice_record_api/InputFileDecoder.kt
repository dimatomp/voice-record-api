package net.dimatomp.voice_record_api

import javax.sound.sampled.AudioInputStream

fun interface InputFileDecoder {
    fun decode(fileContent: ByteArray): AudioInputStream
}