package net.dimatomp.voice_record_api

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.core.io.ResourceLoader
import org.springframework.http.MediaType
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.jdbc.core.JdbcTemplate
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.io.File

@SpringBootTest
@AutoConfigureMockMvc
class VoiceRecordApiApplicationTests {

	@Autowired
	private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var resLoader: ResourceLoader

	@Autowired
	private lateinit var jdbc: JdbcTemplate

	@BeforeEach
	fun startUp() {
		jdbc.execute("DELETE FROM voice_records WHERE user_id = 1 AND phrase_id = 1")
	}

	@Test
	fun shouldStoreAndServeLongAacRecords() {
		resLoader.getResource("classpath:input_long.m4a").inputStream.use { aacInput ->
			mvc.perform(multipart("/audio/user/1/phrase/1").file(MockMultipartFile("audio_file", aacInput))).run { 
				andExpect(status().isCreated)
			}
		}

		val asyncGet = mvc.perform(get("/audio/user/1/phrase/1/m4a")).run {
			andExpect(status().isOk)
			andExpect(content().contentType("audio/mp4"))
			andExpect(request().asyncStarted())
			andReturn()
		}
		mvc.perform(asyncDispatch(asyncGet)).andReturn().response.contentAsByteArray.let {
			val probe = FFProbeVerifier.verify(ByteArrayInputStream(it))
			assert(probe.contains("Duration: 00:01")) { "Returned record duration is not 1 minute\n\n$probe" }
			assert(probe.contains("Input #0, mov,mp4,m4a")) { "Returned record is not an MP4 container\n\n$probe" }
			assert(probe.contains("Audio: aac")) { "Returned record does not use AAC codec\n\n$probe" }
		}
	}
}
