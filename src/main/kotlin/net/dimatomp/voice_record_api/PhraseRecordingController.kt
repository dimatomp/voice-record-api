package net.dimatomp.voice_record_api

import org.jcodec.common.io.ByteBufferSeekableByteChannel
import org.jcodec.common.model.Packet
import org.jcodec.containers.mp4.demuxer.MP4Demuxer
import org.mp4parser.muxer.InMemRandomAccessSourceImpl
import org.mp4parser.muxer.container.mp4.MovieCreator
import org.mp4parser.tools.ByteBufferByteChannel
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
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
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
            val demuxer = MP4Demuxer.createMP4Demuxer(ByteBufferSeekableByteChannel(ByteBuffer.wrap(bulkFileRead), bulkFileRead.size))
            demuxer.audioTracks
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity("Invalid media format, only .m4a files are supported", HttpStatus.BAD_REQUEST)
        }
        if (tracks.size != 1)
            return ResponseEntity("No audio track found in MP4 file", HttpStatus.BAD_REQUEST);
        val pipeIn = PipedInputStream()
        val writeFuture = this.executor.submit {
            try {
                val encoded = AudioSystem.getAudioInputStream(BufferedInputStream(pipeIn))
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        PipedOutputStream(pipeIn).use {
            FileChannel.open(Path.of("test.aac"), StandardOpenOption.WRITE, StandardOpenOption.CREATE).use { file ->
                val outChannel = Channels.newChannel(it)
                val track = tracks[0]
                var packet: Packet?
                do {
                    packet = track.nextFrame()
                    packet?.data?.let {
                        //outChannel.write(packet.data)
                        file.write(it)
                    }
                } while (packet != null)
            }
        }
        writeFuture.get()
        return ResponseEntity(HttpStatus.CREATED)
    }
}