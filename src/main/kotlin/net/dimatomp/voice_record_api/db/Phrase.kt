package net.dimatomp.voice_record_api.db

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Column
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@Table(name = "phrases")
data class Phase(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", nullable = false) var id: Int
)

interface PhraseRepository: JpaRepository<Phase, Int>