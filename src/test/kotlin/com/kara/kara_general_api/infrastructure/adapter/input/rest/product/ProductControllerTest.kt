package com.kara.kara_general_api.infrastructure.adapter.input.rest.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.product.CreateProductUseCase
import com.kara.kara_general_api.domain.port.input.product.DeleteProductResult
import com.kara.kara_general_api.domain.port.input.product.DeleteProductUseCase
import com.kara.kara_general_api.domain.port.input.product.ListProductsUseCase
import com.kara.kara_general_api.domain.port.input.product.UpdateProductResult
import com.kara.kara_general_api.domain.port.input.product.UpdateProductUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

private const val PRODUCT_ID = "44444444-4444-4444-4444-444444444441"
private const val REQUEST_BODY =
    """{"name": "Coca-Cola 33cl", "description": "Canette 33cl", "price": 2.50, "currency": "EUR"}"""

@WebMvcTest(ProductController::class)
@Import(SecurityConfig::class)
class ProductControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createProductUseCase: CreateProductUseCase

    @MockkBean
    private lateinit var listProductsUseCase: ListProductsUseCase

    @MockkBean
    private lateinit var updateProductUseCase: UpdateProductUseCase

    @MockkBean
    private lateinit var deleteProductUseCase: DeleteProductUseCase

    private val product =
        Product(
            id = ProductId(UUID.fromString(PRODUCT_ID)),
            name = "Coca-Cola 33cl",
            description = "Canette 33cl",
            price = BigDecimal("2.50"),
            currency = Currency.EUR,
        )

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 201 when admin creates a product`() {
        every { createProductUseCase.createProduct(any()) } returns product

        mockMvc
            .perform(
                post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(PRODUCT_ID))
            .andExpect(jsonPath("$.name").value("Coca-Cola 33cl"))
            .andExpect(jsonPath("$.price").value(2.50))
            .andExpect(jsonPath("$.currency").value("EUR"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 400 when the create body fails bean validation`() {
        val invalidBody = """{"name": "  ", "description": null, "price": -5, "currency": "EUR"}"""

        mockMvc
            .perform(
                post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))

        verify(exactly = 0) { createProductUseCase.createProduct(any()) }
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin creates a product`() {
        mockMvc
            .perform(
                post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return 401 when unauthenticated creates a product`() {
        mockMvc
            .perform(
                post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 200 with the catalog when admin lists products`() {
        every { listProductsUseCase.listProducts() } returns listOf(product)

        mockMvc
            .perform(get("/api/v1/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Coca-Cola 33cl"))
    }

    @Test
    @WithMockUser(roles = ["SERVER"])
    fun `should return 200 when a server lists products to stock a room`() {
        every { listProductsUseCase.listProducts() } returns listOf(product)

        mockMvc
            .perform(get("/api/v1/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when a client lists products`() {
        mockMvc
            .perform(get("/api/v1/products"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 200 when admin updates a product`() {
        every { updateProductUseCase.updateProduct(any()) } returns
            UpdateProductResult.Success(product.copy(name = "Coca-Cola Zero 33cl"))

        mockMvc
            .perform(
                patch("/api/v1/products/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Coca-Cola Zero 33cl"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Coca-Cola Zero 33cl"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 400 when the update body fails bean validation`() {
        mockMvc
            .perform(
                patch("/api/v1/products/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"price": -5}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))

        verify(exactly = 0) { updateProductUseCase.updateProduct(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 404 when admin updates an unknown product`() {
        every { updateProductUseCase.updateProduct(any()) } returns UpdateProductResult.NotFound

        mockMvc
            .perform(
                patch("/api/v1/products/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "x"}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin updates a product`() {
        mockMvc
            .perform(
                patch("/api/v1/products/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "x"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return 401 when unauthenticated updates a product`() {
        mockMvc
            .perform(
                patch("/api/v1/products/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "x"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 204 when admin deletes a product`() {
        every { deleteProductUseCase.deleteProduct(ProductId(UUID.fromString(PRODUCT_ID))) } returns
            DeleteProductResult.Success

        mockMvc
            .perform(delete("/api/v1/products/$PRODUCT_ID"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 404 when admin deletes an unknown product`() {
        every { deleteProductUseCase.deleteProduct(ProductId(UUID.fromString(PRODUCT_ID))) } returns
            DeleteProductResult.NotFound

        mockMvc
            .perform(delete("/api/v1/products/$PRODUCT_ID"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin deletes a product`() {
        mockMvc
            .perform(delete("/api/v1/products/$PRODUCT_ID"))
            .andExpect(status().isForbidden)
    }
}
