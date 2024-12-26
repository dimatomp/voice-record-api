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
import net.dimatomp.voice_record_api.db.UserRepository
import net.dimatomp.voice_record_api.db.PhraseRepository


@Controller
@RequestMapping("/audio/user/{userId}/phrase/{phraseId}")
class PhraseRecordingController @Autowired constructor(
    private val converters: Collection<InputFileDecoder>,
    private val aacMp4Encoder: AacMp4Encoder,
    private val userRepo: UserRepository,
    private val phraseRepo: PhraseRepository
) {
    companion object {
        private val log = LogFactory.getLog(PhraseRecordingController::class.java)
    }

    @PostMapping
    fun storeRecord(@PathVariable userId: Int, @PathVariable phraseId: Int, @RequestParam("audio_file") file: MultipartFile): ResponseEntity<String> {
        try {
            validate(userId, phraseId)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity(e.message, HttpStatus.BAD_REQUEST)
        }
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
    fun fetchRecord(@PathVariable userId: Int, @PathVariable phraseId: Int, @PathVariable audioFormat: String): ResponseEntity<StreamingResponseBody> {
        try {
            validate(userId, phraseId)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        if (audioFormat != "m4a") return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity.ok().run {
            header("Content-Type", "audio/mp4")
            body(StreamingResponseBody { out ->
                FileInputStream("test.wav").use { input -> aacMp4Encoder.encode(input, out) }
            })
        }
    }

    private fun validate(userId: Int, phraseId: Int) {
        require(userRepo.existsById(userId)) { "User not found" }
        require(phraseRepo.existsById(phraseId)) { "Phrase not found" }
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