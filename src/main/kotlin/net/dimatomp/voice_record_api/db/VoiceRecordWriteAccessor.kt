package net.dimatomp.voice_record_api.db

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.io.InputStream

@Component
class VoiceRecordWriteAccessor @Autowired constructor(
    private val jdbc: JdbcTemplate
) {
    fun saveRecord(userId: Int, phraseId: Int, record: InputStream) {
        jdbc.execute("INSERT INTO voice_records (user_id, phrase_id, content) VALUES (?, ?, ?)") {
            it.setInt(1, userId)
            it.setInt(2, phraseId)
            it.setBinaryStream(3, record)
            it.executeUpdate()
        }
    }
}