package com.kara.kara_general_api.domain.port.input.product

import com.kara.kara_general_api.domain.model.product.ProductId

sealed interface DeleteProductResult {
    data object Success : DeleteProductResult

    data object NotFound : DeleteProductResult
}

interface DeleteProductUseCase {
    fun deleteProduct(id: ProductId): DeleteProductResult
}
