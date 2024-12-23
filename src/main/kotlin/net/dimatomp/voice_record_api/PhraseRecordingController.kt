package net.dimatomp.voice_record_api

import org.mp4parser.muxer.InMemRandomAccessSourceImpl
import org.mp4parser.muxer.container.mp4.MovieCreator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.nio.channels.Channels
import javax.sound.sampled.*


@Controller
@RequestMapping("/audio/user/{userId}/phrase/{phraseId}")
class PhraseRecordingController @Autowired constructor(private val executor: ThreadPoolTaskExecutor) {
    @PostMapping()
    fun storeRecord(@PathVariable userId: String, @PathVariable phraseId: String, @RequestParam("audio_file") file: MultipartFile): ResponseEntity<Any> {
        val tracks = try {
            // Sadly there does not seem to be any modern Java library for _streamed_ parsing of MP4;
            // resorting to reading the entire input before processing.
            // TODO Add validation rules to limit input size according to config
            val bulkFileRead = file.resource.contentAsByteArray
            val movie = MovieCreator.build(Channels.newChannel(ByteArrayInputStream(bulkFileRead)), InMemRandomAccessSourceImpl(bulkFileRead), file.name)
            movie.tracks
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity("Invalid media format, only .m4a files are supported", HttpStatus.BAD_REQUEST)
        }
        if (tracks.size != 1 || tracks[0].handler != "soun")
            return ResponseEntity("Only regular audio files are supported", HttpStatus.BAD_REQUEST);
        val pipeIn = PipedInputStream()
        val writeFuture = this.executor.submit {
            try {
                BufferedInputStream(FileInputStream("input.aac")).use { fileInput ->
                    val encoded = AudioSystem.getAudioInputStream(fileInput)
                    val encodedFormat = encoded.format
                    val decoded = AudioSystem.getAudioInputStream(
                        AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED, encodedFormat.sampleRate, 16,
                            encodedFormat.channels, 2 * encodedFormat.channels, encodedFormat.frameRate,
                            encodedFormat.isBigEndian, encodedFormat.properties()
                        ), encoded
                    )
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val pipeOut = Channels.newChannel(PipedOutputStream(pipeIn))
        for (sample in tracks[0].samples) {
            sample.writeTo(pipeOut)
        }
        pipeOut.close()
        writeFuture.get()
        return ResponseEntity(HttpStatus.CREATED)
    }
}