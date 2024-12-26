package net.dimatomp.voice_record_api.codec

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.core.io.ResourceLoader
import org.junit.Before
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

    @Before
    fun setUp() {
        val process = ProcessBuilder("ffprobe", "-version").run {
            redirectOutput(ProcessBuilder.Redirect.INHERIT)
            redirectError(ProcessBuilder.Redirect.INHERIT)
            start()
        }
        process.waitFor(1, TimeUnit.SECONDS)
        check(process.exitValue() == 0) { "ffprobe did not respond successfully when asked for its version" }
    }

    @ParameterizedTest(name = "Should convert M4A container with {argumentSetName} stream to WAV")
    @MethodSource("shouldProcessM4aTestCases")
    fun shouldProcessM4a(filename: String) {
        val wavStream = resLoader.getResource(filename).inputStream.use {
            converter.convertToWav(MockMultipartFile(filename, it))
        }
        val process = ProcessBuilder("ffprobe", "-").run {
            redirectInput(ProcessBuilder.Redirect.PIPE)
            redirectError(ProcessBuilder.Redirect.PIPE)
            start()
        }
        wavStream.use {
            try {
                it.transferTo(process.outputStream)
                process.outputStream.close()
            } catch (e: IOException) {
                // This might be ok because ffprobe does not read entire file content
            }
        }
        val probe = process.errorStream.reader().use { it.readText() }
        process.waitFor(1, TimeUnit.SECONDS)
        assert(process.exitValue() == 0) { "ffprobe did not recognize format of converted audio stream" }
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