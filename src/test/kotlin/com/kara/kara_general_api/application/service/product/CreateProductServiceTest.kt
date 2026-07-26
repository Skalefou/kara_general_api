package com.kara.kara_general_api.application.service.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.product.CreateProductCommand
import com.kara.kara_general_api.domain.port.output.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class CreateProductServiceTest {
    private val productRepository = mockk<ProductRepository>()
    private val sut = CreateProductService(productRepository)

    @Test
    fun `should generate an id and persist the new product`() {
        val saved = slot<Product>()
        every { productRepository.save(capture(saved)) } answers { saved.captured }

        val result =
            sut.createProduct(
                CreateProductCommand(
                    name = "Coca-Cola 33cl",
                    description = "Canette 33cl",
                    price = BigDecimal("2.50"),
                    currency = Currency.EUR,
                ),
            )

        verify(exactly = 1) { productRepository.save(any()) }
        assertEquals("Coca-Cola 33cl", result.name)
        assertEquals(BigDecimal("2.50"), result.price)
        assertEquals(Currency.EUR, result.currency)
        assertEquals(saved.captured.id, result.id)
    }
}
