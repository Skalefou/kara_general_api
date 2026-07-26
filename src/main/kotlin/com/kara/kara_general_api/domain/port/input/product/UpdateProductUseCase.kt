package com.kara.kara_general_api.domain.port.input.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import java.math.BigDecimal

/**
 * Mise à jour partielle d'un produit du catalogue générique : chaque champ non-null remplace la
 * valeur existante ; un champ null laisse la valeur inchangée.
 */
data class UpdateProductCommand(
    val id: ProductId,
    val name: String?,
    val description: String?,
    val price: BigDecimal?,
    val currency: Currency?,
)

sealed interface UpdateProductResult {
    data class Success(val product: Product) : UpdateProductResult

    data object NotFound : UpdateProductResult
}

interface UpdateProductUseCase {
    fun updateProduct(command: UpdateProductCommand): UpdateProductResult
}
