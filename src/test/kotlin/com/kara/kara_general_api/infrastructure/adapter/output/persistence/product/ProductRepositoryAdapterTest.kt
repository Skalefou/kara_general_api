package com.kara.kara_general_api.infrastructure.adapter.output.persistence.product

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
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

// Le schéma est généré par Hibernate depuis les @Entity (ProductEntity) ; on impose ddl-auto ici
// pour matérialiser les tables dans le conteneur PostgreSQL.
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class ProductRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: ProductRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    @BeforeEach
    fun cleanProducts() {
        jdbc.update("DELETE FROM products", emptyMap<String, Any>())
    }

    private fun product(
        name: String,
        price: String,
    ): Product =
        Product(
            id = ProductId(UUID.randomUUID()),
            name = name,
            description = "Description $name",
            price = BigDecimal(price),
            currency = Currency.EUR,
        )

    @Test
    fun `save then findById returns the persisted product`() {
        val saved = adapter.save(product("Coca-Cola 33cl", "2.50"))

        val found = adapter.findById(saved.id)

        assertEquals(saved, found)
    }

    @Test
    fun `save upserts an existing product by id`() {
        val original = adapter.save(product("Coca-Cola", "2.50"))
        adapter.save(original.copy(name = "Coca-Cola Zero", price = BigDecimal("2.80")))

        val found = adapter.findById(original.id)

        assertEquals("Coca-Cola Zero", found?.name)
        assertEquals(BigDecimal("2.80"), found?.price)
    }

    @Test
    fun `findAll returns the whole catalog ordered by name`() {
        adapter.save(product("Part de pizza", "4.00"))
        adapter.save(product("Coca-Cola 33cl", "2.50"))
        adapter.save(product("Eau minérale 50cl", "1.50"))

        val result = adapter.findAll()

        assertEquals(listOf("Coca-Cola 33cl", "Eau minérale 50cl", "Part de pizza"), result.map { it.name })
    }

    @Test
    fun `existsById reflects presence of the product`() {
        val saved = adapter.save(product("Eau minérale 50cl", "1.50"))

        assertTrue(adapter.existsById(saved.id))
        assertFalse(adapter.existsById(ProductId(UUID.randomUUID())))
    }

    @Test
    fun `deleteById removes the product and returns true, false when absent`() {
        val saved = adapter.save(product("Eau minérale 50cl", "1.50"))

        assertTrue(adapter.deleteById(saved.id))
        assertNull(adapter.findById(saved.id))
        assertFalse(adapter.deleteById(saved.id))
    }
}
