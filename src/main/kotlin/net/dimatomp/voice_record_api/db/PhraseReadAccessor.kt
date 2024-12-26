package net.dimatomp.voice_record_api.db

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.Types

@Component
class PhraseReadAccessor @Autowired constructor(private val jdbc: JdbcTemplate) {
    fun doesPhraseExist(id: Int): Boolean {
        return jdbc.query(
            "SELECT EXISTS(SELECT 1 FROM phrases WHERE id = ?)",
            arrayOf(id), intArrayOf(Types.INTEGER),
            RowMapper { resultSet, _ -> resultSet.getBoolean(1) }
        )[0]
    }
}