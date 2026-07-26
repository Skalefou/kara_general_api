package com.kara.kara_general_api.application.service.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.output.ProductRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

class ListProductsServiceTest {
    private val productRepository = mockk<ProductRepository>()
    private val sut = ListProductsService(productRepository)

    @Test
    fun `should return the generic catalog from the repository`() {
        val products =
            listOf(
                Product(ProductId(UUID.randomUUID()), "Coca-Cola 33cl", null, BigDecimal("2.50"), Currency.EUR),
                Product(ProductId(UUID.randomUUID()), "Part de pizza", null, BigDecimal("4.00"), Currency.EUR),
            )
        every { productRepository.findAll() } returns products

        assertEquals(products, sut.listProducts())
    }
}
