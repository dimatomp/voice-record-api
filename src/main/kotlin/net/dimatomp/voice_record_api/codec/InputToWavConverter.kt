package net.dimatomp.voice_record_api.codec

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.multipart.MultipartFile
import org.apache.commons.logging.LogFactory
import javax.sound.sampled.UnsupportedAudioFileException
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFileFormat
import java.io.IOException
import java.io.OutputStream

@Component
class InputToWavConverter @Autowired constructor(private val converters: Collection<InputFileDecoder>) {
    companion object {
        private val log = LogFactory.getLog(InputToWavConverter::class.java)
    }

    fun convertToWav(input: MultipartFile, output: OutputStream) {
        val decoded = findDecoder(input.bytes)
        require(decoded != null) { "Unsupported file format" }
        decoded.use {
            val decodedFormat = decoded.format
            val stored = AudioSystem.getAudioInputStream(
                AudioFormat(
                    AudioFormat.Encoding.PCM_FLOAT, decodedFormat.sampleRate, 32,
                    1, 4, decodedFormat.frameRate,
                    decodedFormat.isBigEndian, decodedFormat.properties()
                ), decoded
            )
            AudioSystem.write(stored, AudioFileFormat.Type.WAVE, output)
        }
    }

    private fun findDecoder(content: ByteArray): AudioInputStream? {
        for (decoder in this.converters) {
            try {
                return decoder.decode(content)
            } catch (e: Exception) {
                when (e) {
                    is UnsupportedAudioFileException, is IOException, is IllegalArgumentException -> {
                        // Errors are ok, this is how decoders will indicate incompatible format
                        // (similar to javax.sound.sampled.AudioSystem)
                        log.debug(e)
                    }
                }
            }
        }
        return null
    }
}