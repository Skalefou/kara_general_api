package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.UserBooking
import com.kara.kara_general_api.domain.model.booking.UserBookingOption
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.room.vo.Address
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

/** Libellé de repli lorsque la salle d'une réservation n'existe plus. */
private const val UNKNOWN_ROOM_NAME = "Salle"

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

    override fun findByUserInvolvement(userId: UserId): List<UserBooking> {
        // « Mes événements » : tous les statuts, du plus récent créneau au plus ancien. Deux rôles donnent
        // accès à une réservation — organisateur (b.user_id) ou détenteur d'une part de sa cagnotte
        // (pool_shares.payer_user_id) : même sémantique d'implication que PoolRepositoryAdapter
        // .findByUserInvolvement. Le test d'implication est écrit en EXISTS (et non en LEFT JOIN) : la
        // réservation reste sur une seule ligne même lorsque l'utilisateur détient plusieurs parts, sans
        // DISTINCT à concilier avec l'ORDER BY. La salle est jointe en LEFT JOIN pour survivre à une salle
        // supprimée (nom retombant sur un libellé neutre).
        val sql =
            """
            SELECT b.id, b.room_id, b.user_id, b.start_at, b.end_at, b.number_of_people,
                   b.total_price, b.currency, b.status, b.payment_mode, b.created_at, b.expires_at,
                   r.name AS room_name, r.street AS room_street, r.city AS room_city,
                   r.postal_code AS room_postal_code, r.country AS room_country
            FROM bookings b
            LEFT JOIN rooms r ON r.id = b.room_id
            WHERE b.user_id = :userId
               OR EXISTS (
                    SELECT 1
                    FROM pool_shares s
                    JOIN pools p ON p.id = s.pool_id
                    WHERE p.booking_id = b.id
                      AND s.payer_user_id = :userId
               )
            ORDER BY b.start_at DESC
            """.trimIndent()
        val rows =
            jdbc.query(sql, mapOf("userId" to userId.value)) { rs, rowNum ->
                UserBooking(
                    booking = rowMapper.mapRow(rs, rowNum),
                    roomName = rs.getString("room_name") ?: UNKNOWN_ROOM_NAME,
                    roomAddress =
                        rs.getString("room_street")?.let { street ->
                            Address(
                                street = street,
                                city = rs.getString("room_city"),
                                postalCode = rs.getString("room_postal_code"),
                                country = rs.getString("room_country"),
                            )
                        },
                    options = emptyList(),
                    isCreator = rs.getObject("user_id", UUID::class.java) == userId.value,
                )
            }
        // Aucune réservation ⇒ aucune requête supplémentaire.
        if (rows.isEmpty()) return emptyList()
        val optionsByBooking = findOptionsByBookingIds(rows.map { it.booking.id })
        return rows.map { row ->
            val options = optionsByBooking[row.booking.id].orEmpty()
            row.copy(
                booking = row.booking.copy(selectedOptionIds = options.map { it.optionId }),
                options = options,
            )
        }
    }

    /**
     * Options de **toutes** les réservations demandées en une seule requête (`IN (:bookingIds)`) : les
     * identifiants figés dans `booking_options` sont joints au catalogue `services` pour leur libellé et
     * leur prix forfaitaire. Évite une requête par réservation (N+1).
     */
    private fun findOptionsByBookingIds(bookingIds: List<BookingId>): Map<BookingId, List<UserBookingOption>> {
        if (bookingIds.isEmpty()) return emptyMap()
        val sql =
            """
            SELECT bo.booking_id AS booking_id, s.id AS option_id, s.label AS label,
                   s.price AS price, s.currency AS currency
            FROM booking_options bo
            JOIN services s ON s.id = bo.option_id
            WHERE bo.booking_id IN (:bookingIds)
            ORDER BY bo.created_at ASC
            """.trimIndent()
        val params = MapSqlParameterSource().addValue("bookingIds", bookingIds.map { it.value })
        return jdbc
            .query(sql, params) { rs, _ ->
                BookingId(rs.getObject("booking_id", UUID::class.java)) to
                    UserBookingOption(
                        optionId = RoomOptionId(rs.getObject("option_id", UUID::class.java)),
                        label = rs.getString("label"),
                        price = rs.getBigDecimal("price"),
                        currency = Currency.valueOf(rs.getString("currency")),
                    )
            }.groupBy({ it.first }, { it.second })
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
        // Le NOT EXISTS protège une réservation effectivement payée dont la confirmation n'a pas encore été
        // appliquée (webhook Stripe non arrivé) : on ne doit jamais annuler un créneau déjà encaissé.
        val sql =
            """
            UPDATE bookings
            SET status = 'CANCELLED'
            WHERE status = 'PENDING'
              AND payment_mode = 'PAY_ALL'
              AND expires_at <= :now
              AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id = bookings.id AND p.status = 'PAID')
            """.trimIndent()
        return jdbc.update(sql, MapSqlParameterSource().addValue("now", Timestamp.from(now)))
    }
}
