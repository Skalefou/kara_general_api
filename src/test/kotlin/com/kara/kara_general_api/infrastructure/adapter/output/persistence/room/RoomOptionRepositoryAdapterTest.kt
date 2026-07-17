package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.util.UUID

// Le schéma est généré par Hibernate depuis les @Entity (RoomOptionEntity) ; on impose ddl-auto ici
// pour matérialiser les tables dans le conteneur PostgreSQL.
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class RoomOptionRepositoryAdapterTest {

    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @Autowired
    private lateinit var adapter: RoomOptionRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val roomA = RoomId(UUID.randomUUID())
    private val roomB = RoomId(UUID.randomUUID())

    @BeforeEach
    fun cleanOptions() {
        jdbc.update("DELETE FROM room_options", emptyMap<String, Any>())
    }

    @Test
    fun `findByRoomId returns only the options of the given room ordered by label`() {
        insertOption(roomA, "Ménage fin de soirée", "60.00")
        insertOption(roomA, "Agent de sécurité", "25.00")
        insertOption(roomB, "DJ Set (4h)", "300.00")

        val result = adapter.findByRoomId(roomA)

        assertEquals(listOf("Agent de sécurité", "Ménage fin de soirée"), result.map { it.label })
        assertEquals(BigDecimal("25.00"), result.first().price)
        assertEquals(roomA, result.first().roomId)
    }

    @Test
    fun `findByRoomId returns an empty list when the room has no option`() {
        insertOption(roomB, "DJ Set (4h)", "300.00")

        assertTrue(adapter.findByRoomId(roomA).isEmpty())
    }

    private fun insertOption(roomId: RoomId, label: String, price: String) {
        val sql =
            """
            INSERT INTO room_options (id, room_id, label, description, price, currency, created_at)
            VALUES (:id, :roomId, :label, :description, :price, :currency, NOW())
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("roomId", roomId.value)
                .addValue("label", label)
                .addValue("description", "Description $label")
                .addValue("price", BigDecimal(price))
                .addValue("currency", "EUR"),
        )
    }
}
