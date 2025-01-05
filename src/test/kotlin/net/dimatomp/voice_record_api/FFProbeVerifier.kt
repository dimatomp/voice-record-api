package net.dimatomp.voice_record_api

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

object FFProbeVerifier {
    init {
        val process = ProcessBuilder("ffprobe", "-version").run {
            redirectOutput(ProcessBuilder.Redirect.INHERIT)
            redirectError(ProcessBuilder.Redirect.INHERIT)
            start()
        }
        process.waitFor(1, TimeUnit.MINUTES)
        check(process.exitValue() == 0) { "ffprobe did not respond successfully when asked for its version" }
    }
    
    fun verify(audioStream: InputStream): String {
        val process = ProcessBuilder("ffprobe", "-").run {
            redirectInput(ProcessBuilder.Redirect.PIPE)
            redirectError(ProcessBuilder.Redirect.PIPE)
            start()
        }
        audioStream.use {
            try {
                it.transferTo(process.outputStream)
                process.outputStream.close()
            } catch (e: IOException) {
                // This might be ok because ffprobe does not read entire file content
            }
        }
        val probe = process.errorStream.reader().use { it.readText() }
        process.waitFor(1, TimeUnit.SECONDS)
        assert(process.exitValue() == 0) { "ffprobe did not recognize format of converted audio stream.\n\n$probe" }
        return probe
    }
}
