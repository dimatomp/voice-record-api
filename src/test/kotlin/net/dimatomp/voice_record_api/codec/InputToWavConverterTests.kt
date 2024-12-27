package net.dimatomp.voice_record_api.codec

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.core.io.ResourceLoader
import org.junit.Before
import net.dimatomp.voice_record_api.FFProbeVerifier
import net.dimatomp.voice_record_api.codec.InputToWavConverter
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import java.io.IOException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.FieldSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.ParameterizedTest

@SpringBootTest
class InputToWavConverterTests {
    @Autowired 
    private lateinit var converter: InputToWavConverter

    @Autowired
    private lateinit var resLoader: ResourceLoader

    @ParameterizedTest(name = "Should convert M4A container with {argumentSetName} stream to WAV")
    @MethodSource("shouldProcessM4aTestCases")
    fun shouldProcessM4a(filename: String) {
        val probe = resLoader.getResource(filename).inputStream.use {
            val wavStream = converter.convertToWav(MockMultipartFile(filename, it))
            FFProbeVerifier.verify(wavStream)
        }
        assert(probe.contains("Input #0, wav")) { "ffprobe does not think this is a valid wav output.\n\n$probe" }
        assert(probe.contains("Audio: pcm_f32le ([3][0][0][0] / 0x0003), 44100 Hz, 1 channels")) { "ffprobe does not think this is a valid pcm output.\n\n$probe" }
    }

    companion object {
        @JvmStatic
        fun shouldProcessM4aTestCases(): Stream<Arguments> {
            return Stream.of(
                Arguments.argumentSet("AAC", "classpath:input.m4a"),
                Arguments.argumentSet("ALAC", "classpath:input_alac.m4a")
            )
        }
    }
}