package com.kara.kara_general_api.domain.port.input.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.room.Currency
import java.math.BigDecimal

data class CreateProductCommand(
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val currency: Currency,
)

interface CreateProductUseCase {
    fun createProduct(command: CreateProductCommand): Product
}
