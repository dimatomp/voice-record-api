package net.dimatomp.voice_record_api.db

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.io.InputStream
import java.sql.Types

@Component
class VoiceRecordReadAccessor @Autowired constructor(private val jdbc: JdbcTemplate) {
    fun <T> readVoiceRecord(userId: Int, phraseId: Int, callback: (InputStream?) -> T): T {
        return jdbc.query(
            "SELECT content FROM voice_records WHERE user_id = ? AND phrase_id = ? ORDER BY id DESC LIMIT 1", 
            arrayOf(userId, phraseId), intArrayOf(Types.INTEGER, Types.INTEGER),
            ResultSetExtractor { callback(if (it.next()) it.getBinaryStream(1) else null) }
        ) as T
    }
}