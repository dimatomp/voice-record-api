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
import net.dimatomp.voice_record_api.db.UserReadAccessor
import net.dimatomp.voice_record_api.db.PhraseReadAccessor
import net.dimatomp.voice_record_api.db.VoiceRecordWriteAccessor
import net.dimatomp.voice_record_api.codec.InputToWavConverter
import net.dimatomp.voice_record_api.codec.AacMp4Encoder


@Controller
@RequestMapping("/audio/user/{userId}/phrase/{phraseId}")
class PhraseRecordingController @Autowired constructor(
    private val inputConverter: InputToWavConverter,
    private val aacMp4Encoder: AacMp4Encoder,
    private val userRead: UserReadAccessor,
    private val phraseRead: PhraseReadAccessor,
    private val recordWrite: VoiceRecordWriteAccessor,
) {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleInvalidInput(e: IllegalArgumentException): ResponseEntity<String> {
        return ResponseEntity(e.message, HttpStatus.BAD_REQUEST)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun storeRecord(@PathVariable userId: Int, @PathVariable phraseId: Int, @RequestParam("audio_file") file: MultipartFile) {
        validate(userId, phraseId)
        inputConverter.convertToWav(file).use { recordWrite.saveRecord(userId, phraseId, it) }
    }

    @GetMapping("/{audioFormat}")
    fun fetchRecord(@PathVariable userId: Int, @PathVariable phraseId: Int, @PathVariable audioFormat: String): ResponseEntity<StreamingResponseBody> {
        validate(userId, phraseId)
        require(audioFormat == "m4a") { "Unsupported audio format" }
        return ResponseEntity.ok().run {
            header("Content-Type", "audio/mp4")
            body(StreamingResponseBody { out ->
                FileInputStream("test.wav").use { input -> aacMp4Encoder.encode(input, out) }
            })
        }
    }

    private fun validate(userId: Int, phraseId: Int) {
        require(userRead.doesUserExist(userId)) { "User not found" }
        require(phraseRead.doesPhraseExist(phraseId)) { "Phrase not found" }
    }
}