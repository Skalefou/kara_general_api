package com.kara.kara_general_api.application.service.product

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.port.input.product.DeleteProductResult
import com.kara.kara_general_api.domain.port.output.ProductRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class DeleteProductServiceTest {
    private val productRepository = mockk<ProductRepository>()
    private val sut = DeleteProductService(productRepository)

    private val id = ProductId(UUID.randomUUID())

    @Test
    fun `should return Success when the product was deleted`() {
        every { productRepository.deleteById(id) } returns true

        assertEquals(DeleteProductResult.Success, sut.deleteProduct(id))
    }

    @Test
    fun `should return NotFound when no product was deleted`() {
        every { productRepository.deleteById(id) } returns false

        assertEquals(DeleteProductResult.NotFound, sut.deleteProduct(id))
    }
}
