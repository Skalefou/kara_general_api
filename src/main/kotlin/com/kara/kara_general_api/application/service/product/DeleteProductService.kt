package com.kara.kara_general_api.application.service.product

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.port.input.product.DeleteProductResult
import com.kara.kara_general_api.domain.port.input.product.DeleteProductUseCase
import com.kara.kara_general_api.domain.port.output.ProductRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service as SpringService

@SpringService
class DeleteProductService(
    private val productRepository: ProductRepository,
) : DeleteProductUseCase {
    @Transactional
    override fun deleteProduct(id: ProductId): DeleteProductResult {
        val deleted = productRepository.deleteById(id)
        return if (deleted) DeleteProductResult.Success else DeleteProductResult.NotFound
    }
}
