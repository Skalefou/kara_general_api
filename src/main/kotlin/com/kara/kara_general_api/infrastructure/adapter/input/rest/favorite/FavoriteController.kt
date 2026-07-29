package com.kara.kara_general_api.infrastructure.adapter.input.rest.favorite

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteCommand
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteResult
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteUseCase
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoriteRoomIdsUseCase
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoritesQuery
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoritesUseCase
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteCommand
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteResult
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.adapter.input.rest.favorite.dto.FavoriteRoomIdsResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomListResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users/me/favorites")
class FavoriteController(
    private val listFavoritesUseCase: ListFavoritesUseCase,
    private val listFavoriteRoomIdsUseCase: ListFavoriteRoomIdsUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val imageStorage: ImageStoragePort,
) : FavoriteApi {
    override fun listFavorites(
        page: Int,
        size: Int,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val roomPage =
            listFavoritesUseCase.listFavorites(
                ListFavoritesQuery(userId = authentication.userId(), page = page, size = size),
            )
        return ResponseEntity.ok(RoomListResponse.from(roomPage, imageStorage::publicUrl))
    }

    override fun listFavoriteRoomIds(authentication: Authentication): ResponseEntity<Any> =
        ResponseEntity.ok(
            FavoriteRoomIdsResponse.from(listFavoriteRoomIdsUseCase.listFavoriteRoomIds(authentication.userId())),
        )

    override fun addFavorite(
        roomId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> =
        when (addFavoriteUseCase.addFavorite(AddFavoriteCommand(authentication.userId(), RoomId(roomId)))) {
            AddFavoriteResult.Success -> ResponseEntity.noContent().build()
            AddFavoriteResult.RoomNotFound -> roomNotFound()
        }

    override fun removeFavorite(
        roomId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> =
        when (removeFavoriteUseCase.removeFavorite(RemoveFavoriteCommand(authentication.userId(), RoomId(roomId)))) {
            RemoveFavoriteResult.Success -> ResponseEntity.noContent().build()
            RemoveFavoriteResult.NotFound -> favoriteNotFound()
        }

    private fun Authentication.userId(): UserId = UserId(UUID.fromString(name))

    private fun roomNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucune salle ne correspond à cet identifiant.",
                ).apply {
                    title = "Salle introuvable"
                    setProperty("code", "ROOM_NOT_FOUND")
                },
        )

    private fun favoriteNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Cette salle ne fait pas partie de vos favoris.",
                ).apply {
                    title = "Favori introuvable"
                    setProperty("code", "FAVORITE_NOT_FOUND")
                },
        )
}
