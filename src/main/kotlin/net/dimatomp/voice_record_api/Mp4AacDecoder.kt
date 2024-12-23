package net.dimatomp.voice_record_api

import org.apache.commons.logging.LogFactory
import org.jcodec.common.io.ByteBufferSeekableByteChannel
import org.jcodec.containers.mp4.demuxer.MP4Demuxer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.io.BufferedInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

@Component
class Mp4AacDecoder @Autowired constructor(private val executor: ThreadPoolTaskExecutor) : InputFileDecoder {
    companion object {
        private val log = LogFactory.getLog(Mp4AacDecoder::class.java)
    }

    override fun decode(fileContent: ByteArray): AudioInputStream {
        val byteChannel = ByteBufferSeekableByteChannel(ByteBuffer.wrap(fileContent), fileContent.size)
        val audioTracks = MP4Demuxer.createMP4Demuxer(byteChannel).audioTracks
        require(audioTracks.size == 1) { "Should be exactly 1 audio track in MP4 file" }
        val pipeIn = PipedInputStream()
        val pipeOut = PipedOutputStream(pipeIn)
        executor.execute {
            pipeOut.use {
                try {
                    val outChannel = Channels.newChannel(pipeOut)
                    val track = audioTracks[0]
                    do {
                        val packet = track.nextFrame()
                        packet?.data?.let {
                            outChannel.write(packet.data)
                        }
                    } while (packet != null)
                } catch (e: Throwable) {
                    log.debug(e)
                }
            }
        }
        try {
            // relying on io.github.jseproject:jse-spi-aac here
            val encoded = AudioSystem.getAudioInputStream(BufferedInputStream(pipeIn))
            val encodedFormat = encoded.format
            return AudioSystem.getAudioInputStream(
                AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, encodedFormat.sampleRate, 16,
                    encodedFormat.channels, 2 * encodedFormat.channels, encodedFormat.frameRate,
                    encodedFormat.isBigEndian, encodedFormat.properties()
                ), encoded
            )
        } catch (e: Throwable) {
            pipeIn.close()
            throw e
        }
    }
}