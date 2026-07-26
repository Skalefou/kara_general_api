package com.kara.kara_general_api.application.service.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.product.UpdateProductCommand
import com.kara.kara_general_api.domain.port.input.product.UpdateProductResult
import com.kara.kara_general_api.domain.port.output.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateProductServiceTest {
    private val productRepository = mockk<ProductRepository>()
    private val sut = UpdateProductService(productRepository)

    private val id = ProductId(UUID.randomUUID())
    private val existing =
        Product(
            id = id,
            name = "Coca-Cola 33cl",
            description = "Canette 33cl",
            price = BigDecimal("2.50"),
            currency = Currency.EUR,
        )

    @Test
    fun `should apply only the non-null fields and keep the rest unchanged`() {
        every { productRepository.findById(id) } returns existing
        val saved = slot<Product>()
        every { productRepository.save(capture(saved)) } answers { saved.captured }

        val result =
            sut.updateProduct(
                UpdateProductCommand(
                    id = id,
                    name = "Coca-Cola Zero 33cl",
                    description = null,
                    price = BigDecimal("2.80"),
                    currency = null,
                ),
            )

        val success = assertIs<UpdateProductResult.Success>(result)
        assertEquals("Coca-Cola Zero 33cl", success.product.name)
        assertEquals("Canette 33cl", success.product.description)
        assertEquals(BigDecimal("2.80"), success.product.price)
        assertEquals(Currency.EUR, success.product.currency)
        assertEquals(id, success.product.id)
    }

    @Test
    fun `should return NotFound when the product does not exist`() {
        every { productRepository.findById(id) } returns null

        val result =
            sut.updateProduct(
                UpdateProductCommand(id = id, name = "x", description = null, price = null, currency = null),
            )

        assertEquals(UpdateProductResult.NotFound, result)
    }
}
