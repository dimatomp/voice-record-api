package net.dimatomp.voice_record_api.codec

import org.mp4parser.muxer.MemoryDataSourceImpl
import org.mp4parser.muxer.Movie
import org.mp4parser.muxer.builder.DefaultMp4Builder
import org.sheinbergon.aac.sound.AACFileTypes
import org.springframework.stereotype.Component
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import org.mp4parser.muxer.tracks.AACTrackImpl
import java.io.*
import java.nio.channels.Channels

@Component
class AacMp4Encoder {
    fun encode(rawWav: InputStream, m4aOutput: OutputStream) {
        val encoded = AudioSystem.getAudioInputStream(BufferedInputStream(rawWav))
        val encodedFormat = encoded.format
        val decoded = AudioSystem.getAudioInputStream(
            AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED, encodedFormat.sampleRate, 16,
                1, 2, encodedFormat.frameRate,
                false, encodedFormat.properties()
            ), encoded
        )
        val bulkAac = ByteArrayOutputStream()
        AudioSystem.write(decoded, AACFileTypes.AAC_LC, bulkAac)

        val track = AACTrackImpl(MemoryDataSourceImpl(bulkAac.toByteArray()))
        val container = DefaultMp4Builder().build(Movie().apply { addTrack(track) })
        container.writeContainer(Channels.newChannel(m4aOutput))
    }
}