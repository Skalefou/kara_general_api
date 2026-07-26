package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.BookingRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

private const val BOOKING_COLUMNS =
    "id, room_id, user_id, start_at, end_at, number_of_people, total_price, currency, status, " +
        "payment_mode, created_at, expires_at"

@Component
class BookingRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: BookingRowMapper,
) : BookingRepository {
    override fun save(booking: Booking): Booking {
        val sql =
            """
            INSERT INTO bookings (id, room_id, user_id, start_at, end_at, number_of_people,
                                  total_price, currency, status, payment_mode, created_at, expires_at)
            VALUES (:id, :roomId, :userId, :startAt, :endAt, :numberOfPeople,
                    :totalPrice, :currency, :status, :paymentMode, :createdAt, :expiresAt)
            ON CONFLICT (id) DO UPDATE SET
                start_at         = EXCLUDED.start_at,
                end_at           = EXCLUDED.end_at,
                number_of_people = EXCLUDED.number_of_people,
                total_price      = EXCLUDED.total_price,
                currency         = EXCLUDED.currency,
                status           = EXCLUDED.status
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", booking.id.value)
                .addValue("roomId", booking.roomId.value)
                .addValue("userId", booking.userId.value)
                .addValue("startAt", Timestamp.from(booking.startAt))
                .addValue("endAt", Timestamp.from(booking.endAt))
                .addValue("numberOfPeople", booking.numberOfPeople)
                .addValue("totalPrice", booking.totalPrice)
                .addValue("currency", booking.currency.name)
                .addValue("status", booking.status.name)
                .addValue("paymentMode", booking.paymentMode.name)
                .addValue("createdAt", Timestamp.from(booking.createdAt))
                .addValue("expiresAt", Timestamp.from(booking.expiresAt)),
        )
        saveOptions(booking.id, booking.selectedOptionIds)
        return booking
    }

    private fun saveOptions(
        bookingId: BookingId,
        optionIds: List<RoomOptionId>,
    ) {
        if (optionIds.isEmpty()) return
        // ON CONFLICT sur UNIQUE(booking_id, option_id) : ré-enregistrer une option déjà figée est idempotent.
        val sql =
            """
            INSERT INTO booking_options (id, booking_id, option_id, created_at)
            VALUES (:id, :bookingId, :optionId, NOW())
            ON CONFLICT ON CONSTRAINT uq_booking_options_booking_option DO NOTHING
            """.trimIndent()
        val batch =
            optionIds
                .distinct()
                .map { optionId ->
                    MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("bookingId", bookingId.value)
                        .addValue("optionId", optionId.value)
                }.toTypedArray()
        jdbc.batchUpdate(sql, batch)
    }

    override fun findById(id: BookingId): Booking? {
        val sql =
            """
            SELECT $BOOKING_COLUMNS
            FROM bookings
            WHERE id = :id
            """.trimIndent()
        val booking = jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull() ?: return null
        return booking.copy(selectedOptionIds = findOptionIds(id))
    }

    private fun findOptionIds(bookingId: BookingId): List<RoomOptionId> {
        val sql =
            """
            SELECT option_id
            FROM booking_options
            WHERE booking_id = :bookingId
            ORDER BY created_at ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("bookingId" to bookingId.value)) { rs, _ ->
            RoomOptionId(rs.getObject("option_id", UUID::class.java))
        }
    }

    override fun existsOverlapping(
        roomId: RoomId,
        startAt: Instant,
        endAt: Instant,
    ): Boolean {
        // Chevauchement de créneaux : deux intervalles [a,b) et [c,d) se chevauchent ssi a < d ET b > c.
        // Bloquent le créneau : les réservations CONFIRMED et les PENDING non expirées (fenêtre de
        // paiement encore ouverte). Une PENDING expirée libère immédiatement le créneau.
        val sql =
            """
            SELECT COUNT(*)
            FROM bookings
            WHERE room_id = :roomId
              AND (status = 'CONFIRMED' OR (status = 'PENDING' AND expires_at > :now))
              AND start_at < :endAt
              AND end_at > :startAt
            """.trimIndent()
        val count =
            jdbc.queryForObject(
                sql,
                MapSqlParameterSource()
                    .addValue("roomId", roomId.value)
                    .addValue("startAt", Timestamp.from(startAt))
                    .addValue("endAt", Timestamp.from(endAt))
                    .addValue("now", Timestamp.from(Instant.now())),
                Int::class.java,
            ) ?: 0
        return count > 0
    }

    override fun findAssignedToServer(serverId: UserId): List<Booking> {
        // Rattachement serveur→réservation : il existe un créneau d'agenda du serveur, dans la même salle,
        // dont l'intervalle chevauche celui de la réservation ([a,b) et [c,d) se chevauchent ssi a<d ET b>c).
        // DISTINCT car plusieurs créneaux du serveur peuvent couvrir une même réservation.
        val sql =
            """
            SELECT DISTINCT b.id, b.room_id, b.user_id, b.start_at, b.end_at, b.number_of_people,
                   b.total_price, b.currency, b.status, b.created_at, b.expires_at
            FROM bookings b
            JOIN server_shifts s
              ON s.room_id = b.room_id
             AND s.server_id = :serverId
             AND s.start_at < b.end_at
             AND s.end_at > b.start_at
            WHERE b.status <> 'CANCELLED'
            ORDER BY b.start_at ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("serverId" to serverId.value), rowMapper)
    }

    override fun findAllBookings(): List<Booking> {
        val sql =
            """
            SELECT $BOOKING_COLUMNS
            FROM bookings
            ORDER BY start_at DESC
            """.trimIndent()
        return jdbc.query(sql, emptyMap<String, Any>(), rowMapper)
    }

    override fun updateStatus(
        id: BookingId,
        status: BookingStatus,
    ) {
        val sql = "UPDATE bookings SET status = :status WHERE id = :id"
        jdbc.update(sql, mapOf("id" to id.value, "status" to status.name))
    }

    override fun findNextStartAfter(
        roomId: RoomId,
        after: Instant,
        excluding: BookingId,
        now: Instant,
    ): Instant? {
        val sql =
            """
            SELECT MIN(start_at) AS next_start
            FROM bookings
            WHERE room_id = :roomId
              AND id <> :excluding
              AND (status = 'CONFIRMED' OR (status = 'PENDING' AND expires_at > :now))
              AND start_at >= :after
            """.trimIndent()
        return jdbc
            .query(
                sql,
                MapSqlParameterSource()
                    .addValue("roomId", roomId.value)
                    .addValue("excluding", excluding.value)
                    .addValue("after", Timestamp.from(after))
                    .addValue("now", Timestamp.from(now)),
            ) { rs, _ -> rs.getTimestamp("next_start")?.toInstant() }
            .firstOrNull()
    }

    override fun updateEndAt(
        id: BookingId,
        endAt: Instant,
        totalPrice: BigDecimal,
    ) {
        val sql = "UPDATE bookings SET end_at = :endAt, total_price = :totalPrice WHERE id = :id"
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", id.value)
                .addValue("endAt", Timestamp.from(endAt))
                .addValue("totalPrice", totalPrice),
        )
    }

    override fun cancelExpiredPending(now: Instant): Int {
        // Annule les réservations PENDING en mode PAY_ALL dont la fenêtre de paiement (15 min) est échue.
        // Les réservations en mode SHARED_POT sont EXCLUES : c'est le délai de la cagnotte qui gouverne leur
        // annulation (cf. PoolDeadlineScheduler / CancelExpiredPoolsService).
        val sql =
            """
            UPDATE bookings
            SET status = 'CANCELLED'
            WHERE status = 'PENDING'
              AND payment_mode = 'PAY_ALL'
              AND expires_at <= :now
            """.trimIndent()
        return jdbc.update(sql, MapSqlParameterSource().addValue("now", Timestamp.from(now)))
    }
}
