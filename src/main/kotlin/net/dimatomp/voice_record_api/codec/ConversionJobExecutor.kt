package net.dimatomp.voice_record_api.codec

import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Executor
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component

@Component
class ConversionJobExecutor @Autowired constructor(private val executor: ThreadPoolTaskExecutor) {
    companion object {
        private val log = LogFactory.getLog(ConversionJobExecutor::class.java)
    }

    fun convertInParallel(callback: (OutputStream) -> Unit): InputStream {
        val pipeIn = PipedInputStream()
        val pipeOut = PipedOutputStream(pipeIn)
        executor.execute {
            try {
                pipeOut.use { callback(it) }
            } catch (e: Throwable) {
                log.debug(e)
            }
        }
        return pipeIn
    }
}