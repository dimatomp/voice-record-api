package net.dimatomp.voice_record_api

import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

@Component
class AlacFileDecoder: InputFileDecoder {
    override fun decode(fileContent: ByteArray): AudioInputStream {
        // Relying on com.tianscar.javasound:javasound-alac:0.2.3 here.
        // Formats other than ALAC may be correctly processed here as well, but subsequent resampling is not tested
        val encoded = AudioSystem.getAudioInputStream(ByteArrayInputStream(fileContent))
        val encodedFormat = encoded.format
        return AudioSystem.getAudioInputStream(
            AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED, -1.0f, encodedFormat.sampleSizeInBits,
                encodedFormat.channels, -1, -1.0f, encodedFormat.isBigEndian
            ), encoded
        )
    }
}