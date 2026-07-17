package com.kara.kara_general_api.infrastructure.adapter.output.persistence.service

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.util.UUID

// Le schéma est généré par Hibernate depuis les @Entity (ServiceEntity) ; on impose ddl-auto ici
// pour matérialiser les tables dans le conteneur PostgreSQL.
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class ServiceRepositoryAdapterTest {

    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @Autowired
    private lateinit var adapter: ServiceRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    @BeforeEach
    fun cleanServices() {
        jdbc.update("DELETE FROM room_services", emptyMap<String, Any>())
        jdbc.update("DELETE FROM services", emptyMap<String, Any>())
    }

    private fun service(label: String, price: String): Service =
        Service(
            id = ServiceId(UUID.randomUUID()),
            label = label,
            description = "Description $label",
            price = BigDecimal(price),
            currency = Currency.EUR,
        )

    @Test
    fun `save then findById returns the persisted service`() {
        val saved = adapter.save(service("Ménage fin de soirée", "60.00"))

        val found = adapter.findById(saved.id)

        assertEquals(saved, found)
    }

    @Test
    fun `save upserts an existing service by id`() {
        val original = adapter.save(service("DJ Set", "300.00"))
        adapter.save(original.copy(label = "DJ Set (4h)", price = BigDecimal("350.00")))

        val found = adapter.findById(original.id)

        assertEquals("DJ Set (4h)", found?.label)
        assertEquals(BigDecimal("350.00"), found?.price)
    }

    @Test
    fun `findAll returns the whole catalog ordered by label`() {
        adapter.save(service("Ménage fin de soirée", "60.00"))
        adapter.save(service("Agent de sécurité", "25.00"))
        adapter.save(service("DJ Set (4h)", "300.00"))

        val result = adapter.findAll()

        assertEquals(listOf("Agent de sécurité", "DJ Set (4h)", "Ménage fin de soirée"), result.map { it.label })
    }

    @Test
    fun `existsById reflects presence of the service`() {
        val saved = adapter.save(service("Agent de sécurité", "25.00"))

        assertTrue(adapter.existsById(saved.id))
        assertFalse(adapter.existsById(ServiceId(UUID.randomUUID())))
    }

    @Test
    fun `deleteById removes the service and returns true, false when absent`() {
        val saved = adapter.save(service("Agent de sécurité", "25.00"))

        assertTrue(adapter.deleteById(saved.id))
        assertNull(adapter.findById(saved.id))
        assertFalse(adapter.deleteById(saved.id))
    }
}
