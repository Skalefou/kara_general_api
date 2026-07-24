package com.kara.kara_general_api.application.service.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.port.input.product.ListProductsUseCase
import com.kara.kara_general_api.domain.port.output.ProductRepository
import org.springframework.stereotype.Service as SpringService

@SpringService
class ListProductsService(
    private val productRepository: ProductRepository,
) : ListProductsUseCase {

    override fun listProducts(): List<Product> = productRepository.findAll()
}
