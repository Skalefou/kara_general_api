package com.kara.kara_general_api.infrastructure.adapter.input.rest.stock

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockCommand
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockResult
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockUseCase
import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockCommand
import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockResult
import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockUseCase
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockCommand
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockResult
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.stock.dto.RoomStockItemResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.stock.dto.SetRoomStockRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/stock")
class RoomStockController(
    private val getRoomStockUseCase: GetRoomStockUseCase,
    private val setRoomStockUseCase: SetRoomStockUseCase,
    private val removeRoomStockUseCase: RemoveRoomStockUseCase,
) : RoomStockApi {
    override fun getRoomStock(
        roomId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            GetRoomStockCommand(
                roomId = RoomId(roomId),
                currentUserId = currentUserId(authentication),
                isAdmin = isAdmin(authentication),
            )
        return when (val result = getRoomStockUseCase.getRoomStock(command)) {
            is GetRoomStockResult.Success ->
                ResponseEntity.ok(result.entries.map { RoomStockItemResponse.from(it) })
            GetRoomStockResult.RoomNotFound -> roomNotFound()
            GetRoomStockResult.NotAuthorized -> notAuthorized()
        }
    }

    override fun setRoomStock(
        roomId: UUID,
        productId: UUID,
        request: SetRoomStockRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            SetRoomStockCommand(
                roomId = RoomId(roomId),
                productId = ProductId(productId),
                quantity = request.quantity,
                currentUserId = currentUserId(authentication),
                isAdmin = isAdmin(authentication),
            )
        return when (val result = setRoomStockUseCase.setRoomStock(command)) {
            is SetRoomStockResult.Success -> ResponseEntity.ok(RoomStockItemResponse.from(result.entry))
            SetRoomStockResult.RoomNotFound -> roomNotFound()
            SetRoomStockResult.ProductNotFound -> productNotFound()
            SetRoomStockResult.NotAuthorized -> notAuthorized()
        }
    }

    override fun removeRoomStock(
        roomId: UUID,
        productId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            RemoveRoomStockCommand(
                roomId = RoomId(roomId),
                productId = ProductId(productId),
                currentUserId = currentUserId(authentication),
                isAdmin = isAdmin(authentication),
            )
        return when (removeRoomStockUseCase.removeRoomStock(command)) {
            RemoveRoomStockResult.Success -> ResponseEntity.noContent().build()
            RemoveRoomStockResult.RoomNotFound -> roomNotFound()
            RemoveRoomStockResult.NotAuthorized -> notAuthorized()
            RemoveRoomStockResult.NotInStock -> stockItemNotFound()
        }
    }

    private fun currentUserId(authentication: Authentication): UserId = UserId(UUID.fromString(authentication.name))

    private fun isAdmin(authentication: Authentication): Boolean = authentication.authorities.any { it.authority == "ROLE_ADMIN" }

    private fun roomNotFound(): ResponseEntity<Any> =
        problem(HttpStatus.NOT_FOUND, "Salle introuvable", "Aucune salle ne correspond à cet identifiant.", "ROOM_NOT_FOUND")

    private fun productNotFound(): ResponseEntity<Any> =
        problem(HttpStatus.NOT_FOUND, "Produit introuvable", "Aucun produit ne correspond à cet identifiant.", "PRODUCT_NOT_FOUND")

    private fun stockItemNotFound(): ResponseEntity<Any> =
        problem(HttpStatus.NOT_FOUND, "Produit absent du stock", "Ce produit n'est pas au stock de la salle.", "STOCK_ITEM_NOT_FOUND")

    private fun notAuthorized(): ResponseEntity<Any> =
        problem(HttpStatus.FORBIDDEN, "Accès refusé", "Vous n'êtes pas de service dans cette salle.", "NOT_AUTHORIZED")

    private fun problem(
        status: HttpStatus,
        title: String,
        detail: String,
        code: String,
    ): ResponseEntity<Any> =
        ResponseEntity.status(status).body(
            ProblemDetail.forStatusAndDetail(status, detail).apply {
                this.title = title
                setProperty("code", code)
            },
        )
}
