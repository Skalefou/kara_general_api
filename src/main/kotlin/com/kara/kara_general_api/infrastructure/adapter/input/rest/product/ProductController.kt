package com.kara.kara_general_api.infrastructure.adapter.input.rest.product

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.port.input.product.CreateProductCommand
import com.kara.kara_general_api.domain.port.input.product.CreateProductUseCase
import com.kara.kara_general_api.domain.port.input.product.DeleteProductResult
import com.kara.kara_general_api.domain.port.input.product.DeleteProductUseCase
import com.kara.kara_general_api.domain.port.input.product.ListProductsUseCase
import com.kara.kara_general_api.domain.port.input.product.UpdateProductCommand
import com.kara.kara_general_api.domain.port.input.product.UpdateProductResult
import com.kara.kara_general_api.domain.port.input.product.UpdateProductUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.product.dto.CreateProductRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.product.dto.ProductResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.product.dto.UpdateProductRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val createProductUseCase: CreateProductUseCase,
    private val listProductsUseCase: ListProductsUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
) : ProductApi {
    override fun createProduct(request: CreateProductRequest): ResponseEntity<Any> {
        val product =
            createProductUseCase.createProduct(
                CreateProductCommand(
                    name = request.name,
                    description = request.description,
                    price = request.price,
                    currency = request.currency,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product))
    }

    override fun listProducts(): ResponseEntity<Any> =
        ResponseEntity.ok(listProductsUseCase.listProducts().map { ProductResponse.from(it) })

    override fun updateProduct(
        id: UUID,
        request: UpdateProductRequest,
    ): ResponseEntity<Any> {
        val command =
            UpdateProductCommand(
                id = ProductId(id),
                name = request.name,
                description = request.description,
                price = request.price,
                currency = request.currency,
            )
        return when (val result = updateProductUseCase.updateProduct(command)) {
            is UpdateProductResult.Success -> ResponseEntity.ok(ProductResponse.from(result.product))
            UpdateProductResult.NotFound -> productNotFound()
        }
    }

    override fun deleteProduct(id: UUID): ResponseEntity<Any> =
        when (deleteProductUseCase.deleteProduct(ProductId(id))) {
            DeleteProductResult.Success -> ResponseEntity.noContent().build()
            DeleteProductResult.NotFound -> productNotFound()
        }

    private fun productNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucun produit ne correspond à cet identifiant.",
                ).apply {
                    title = "Produit introuvable"
                    setProperty("code", "PRODUCT_NOT_FOUND")
                },
        )
}
