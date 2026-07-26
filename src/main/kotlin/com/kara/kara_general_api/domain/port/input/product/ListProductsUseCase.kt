package com.kara.kara_general_api.domain.port.input.product

import com.kara.kara_general_api.domain.model.product.Product

interface ListProductsUseCase {
    /** Catalogue générique des produits, ordonné par nom. */
    fun listProducts(): List<Product>
}
