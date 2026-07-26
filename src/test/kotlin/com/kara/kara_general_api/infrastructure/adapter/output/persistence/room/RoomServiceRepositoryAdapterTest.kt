package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
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

// Le schéma est généré par Hibernate depuis les @Entity (RoomEntity, ServiceEntity, RoomServiceEntity) ;
// on impose ddl-auto ici pour matérialiser les tables dans le conteneur PostgreSQL. Les FK imposent
// qu'une salle et les services existent avant de créer les liaisons.
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class RoomServiceRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: RoomServiceRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val roomId = RoomId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM room_services", emptyMap<String, Any>())
        jdbc.update("DELETE FROM services", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        insertRoom(roomId)
    }

    @Test
    fun `addLinks then findServiceIdsByRoomId returns the attached services`() {
        val a = insertService("Ménage")
        val b = insertService("Sécurité")

        adapter.addLinks(roomId, listOf(a, b))

        assertEquals(setOf(a, b), adapter.findServiceIdsByRoomId(roomId).toSet())
    }

    @Test
    fun `addLinks is idempotent on the room-service unique constraint`() {
        val a = insertService("Ménage")

        adapter.addLinks(roomId, listOf(a))
        adapter.addLinks(roomId, listOf(a))

        assertEquals(listOf(a), adapter.findServiceIdsByRoomId(roomId))
    }

    @Test
    fun `replaceLinks swaps the whole set of links`() {
        val a = insertService("Ménage")
        val b = insertService("Sécurité")
        val c = insertService("DJ")
        adapter.addLinks(roomId, listOf(a, b))

        adapter.replaceLinks(roomId, listOf(c))

        assertEquals(listOf(c), adapter.findServiceIdsByRoomId(roomId))
    }

    @Test
    fun `replaceLinks with an empty list detaches every service`() {
        val a = insertService("Ménage")
        adapter.addLinks(roomId, listOf(a))

        adapter.replaceLinks(roomId, emptyList())

        assertTrue(adapter.findServiceIdsByRoomId(roomId).isEmpty())
    }

    private fun insertRoom(id: RoomId) {
        val sql =
            """
            INSERT INTO rooms (id, name, description, street, city, postal_code, country,
                               price_per_person_per_hour, currency, max_capacity,
                               is_there_wifi, is_there_sono_pro, is_there_air_conditioning, status, created_at)
            VALUES (:id, 'Salle', 'desc', 'rue', 'Paris', '75002', 'France',
                    10.00, 'EUR', 10, true, false, false, 'OPEN', NOW())
            """.trimIndent()
        jdbc.update(sql, mapOf("id" to id.value))
    }

    private fun insertService(label: String): ServiceId {
        val id = UUID.randomUUID()
        val sql =
            """
            INSERT INTO services (id, label, description, price, currency, created_at)
            VALUES (:id, :label, :description, :price, :currency, NOW())
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", id)
                .addValue("label", label)
                .addValue("description", "Description $label")
                .addValue("price", BigDecimal("25.00"))
                .addValue("currency", "EUR"),
        )
        return ServiceId(id)
    }
}
