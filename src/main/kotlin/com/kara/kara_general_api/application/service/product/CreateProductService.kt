package com.kara.kara_general_api.application.service.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.port.input.product.CreateProductCommand
import com.kara.kara_general_api.domain.port.input.product.CreateProductUseCase
import com.kara.kara_general_api.domain.port.output.ProductRepository
import org.springframework.stereotype.Service as SpringService

@SpringService
class CreateProductService(
    private val productRepository: ProductRepository,
) : CreateProductUseCase {

    override fun createProduct(command: CreateProductCommand): Product {
        val product =
            Product(
                id = ProductId.generate(),
                name = command.name,
                description = command.description,
                price = command.price,
                currency = command.currency,
            )
        return productRepository.save(product)
    }
}
