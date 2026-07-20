package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.BoundingBox
import com.kara.kara_general_api.domain.model.room.vo.Coordinates
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

// Le schéma est généré par Hibernate depuis les @Entity (RoomEntity porte l'index idx_rooms_lat_lng) :
// le profil test ne fixe pas ddl-auto, on l'impose ici pour matérialiser les tables dans le conteneur.
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class RoomRepositoryAdapterTest {

    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: RoomRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    // bbox couvrant grossièrement Paris intra-muros
    private val paris = BoundingBox(minLat = 48.8, minLng = 2.2, maxLat = 48.9, maxLng = 2.4)

    @BeforeEach
    fun cleanRooms() {
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
    }

    @Test
    fun `findInBbox returns only rooms inside the box`() {
        val inside = saveRoom("Salle Centre", latitude = 48.85, longitude = 2.30)
        saveRoom("Salle Nord", latitude = 48.95, longitude = 2.30) // latitude juste au-dessus de maxLat
        saveRoom("Salle Sans Coords", latitude = null, longitude = null)

        val result = adapter.findInBbox(paris, limit = 100)

        assertEquals(listOf(inside.id), result.map { it.id })
    }

    @Test
    fun `countInBbox counts the real number of matching rooms before capping`() {
        repeat(5) { saveRoom("Salle $it", latitude = 48.85, longitude = 2.30) }
        saveRoom("Salle Hors", latitude = 40.0, longitude = 2.30)

        assertEquals(5L, adapter.countInBbox(paris))
    }

    @Test
    fun `findInBbox never returns more than the cap`() {
        repeat(5) { saveRoom("Salle $it", latitude = 48.85, longitude = 2.30) }

        val capped = adapter.findInBbox(paris, limit = 3)

        assertEquals(3, capped.size)
        assertTrue(adapter.countInBbox(paris) > capped.size)
        assertFalse(adapter.countInBbox(paris) <= 3)
    }

    @Test
    fun `clustersInBbox aggregates rooms into non-empty grid cells within the bbox`() {
        // Deux paquets bien séparés dans la bbox : sud-ouest et nord-est.
        repeat(3) { saveRoom("SO $it", latitude = 48.81, longitude = 2.21) }
        repeat(2) { saveRoom("NE $it", latitude = 48.89, longitude = 2.39) }
        // Une salle hors bbox ne doit pas être agrégée.
        saveRoom("Hors", latitude = 40.0, longitude = 2.30)

        val clusters = adapter.clustersInBbox(paris, gridSize = 8)

        // Deux cellules non vides ; les cellules vides sont absentes.
        assertEquals(2, clusters.size)
        // La somme des count == nombre de salles dans la bbox.
        assertEquals(adapter.countInBbox(paris), clusters.sumOf { it.count })
        // Chaque centroïde tombe dans la bbox demandée.
        assertTrue(
            clusters.all {
                it.latitude in paris.minLat..paris.maxLat && it.longitude in paris.minLng..paris.maxLng
            },
        )
    }

    @Test
    fun `findById returns the room with its max capacity`() {
        val saved = saveRoom("Salle Capacité", latitude = 48.85, longitude = 2.30)

        val found = adapter.findById(saved.id)

        assertEquals(50, found?.maxCapacity)
    }

    private fun saveRoom(name: String, latitude: Double?, longitude: Double?): Room {
        val base =
            Room.create(
                name = name,
                description = "Grande salle lumineuse",
                address = Address(street = "1 rue Test", city = "Paris", postalCode = "75001", country = "France"),
                pricePerPersonPerHour = BigDecimal("12.50"),
                currency = Currency.EUR,
                maxCapacity = 50,
                isThereWifi = true,
                isThereSonoPro = false,
                isThereAirConditioning = true,
                coordinates = Coordinates(latitude ?: 0.0, longitude ?: 0.0),
            )
        val room = base.copy(latitude = latitude, longitude = longitude)
        return adapter.save(room)
    }
}
