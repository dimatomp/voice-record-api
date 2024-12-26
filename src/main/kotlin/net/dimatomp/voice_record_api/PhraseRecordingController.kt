package net.dimatomp.voice_record_api

import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.*
import javax.sound.sampled.*


@Controller
@RequestMapping("/audio/user/{userId}/phrase/{phraseId}")
class PhraseRecordingController @Autowired constructor(
    private val converters: Collection<InputFileDecoder>,
    private val aacMp4Encoder: AacMp4Encoder
) {
    companion object {
        private val log = LogFactory.getLog(PhraseRecordingController::class.java)
    }

    @PostMapping
    fun storeRecord(@PathVariable userId: String, @PathVariable phraseId: String, @RequestParam("audio_file") file: MultipartFile): ResponseEntity<String> {
        val decoded = findDecoder(file.bytes) ?: return ResponseEntity("Unsupported file format", HttpStatus.BAD_REQUEST)
        decoded.use {
            val decodedFormat = decoded.format
            val stored = AudioSystem.getAudioInputStream(
                AudioFormat(
                    AudioFormat.Encoding.PCM_FLOAT, decodedFormat.sampleRate, 32,
                    1, 4, decodedFormat.frameRate,
                    decodedFormat.isBigEndian, decodedFormat.properties()
                ), decoded
            )
            FileOutputStream("test.wav").use { AudioSystem.write(stored, AudioFileFormat.Type.WAVE, it) }
        }
        return ResponseEntity(HttpStatus.CREATED)
    }

    @GetMapping("/{audioFormat}")
    fun fetchRecord(@PathVariable userId: String, @PathVariable phraseId: String, @PathVariable audioFormat: String): ResponseEntity<StreamingResponseBody> {
        if (audioFormat != "m4a") return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity.ok().run {
            header("Content-Type", "audio/mp4")
            body(StreamingResponseBody { out ->
                FileInputStream("test.wav").use { input -> aacMp4Encoder.encode(input, out) }
            })
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