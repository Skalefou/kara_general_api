package com.kara.kara_general_api.application.service.product

import com.kara.kara_general_api.domain.port.input.product.UpdateProductCommand
import com.kara.kara_general_api.domain.port.input.product.UpdateProductResult
import com.kara.kara_general_api.domain.port.input.product.UpdateProductUseCase
import com.kara.kara_general_api.domain.port.output.ProductRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service as SpringService

@SpringService
class UpdateProductService(
    private val productRepository: ProductRepository,
) : UpdateProductUseCase {

    @Transactional
    override fun updateProduct(command: UpdateProductCommand): UpdateProductResult {
        val existing = productRepository.findById(command.id) ?: return UpdateProductResult.NotFound
        val updated =
            existing.copy(
                name = command.name ?: existing.name,
                description = if (command.description != null) command.description else existing.description,
                price = command.price ?: existing.price,
                currency = command.currency ?: existing.currency,
            )
        return UpdateProductResult.Success(productRepository.save(updated))
    }
}
