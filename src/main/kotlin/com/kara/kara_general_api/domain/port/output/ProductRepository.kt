package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId

interface ProductRepository {
    fun save(product: Product): Product

    /** Catalogue générique complet, ordonné par nom. */
    fun findAll(): List<Product>

    fun findById(id: ProductId): Product?

    /** Supprime un produit du catalogue. Retourne true si une ligne a été supprimée. */
    fun deleteById(id: ProductId): Boolean

    fun existsById(id: ProductId): Boolean
}
