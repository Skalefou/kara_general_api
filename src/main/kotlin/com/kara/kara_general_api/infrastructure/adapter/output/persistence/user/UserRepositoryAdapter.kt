package com.kara.kara_general_api.infrastructure.adapter.output.persistence.user

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant

private const val USER_COLUMNS =
    "id, email, password_hash, first_name, last_name, phone_number, birth_date, role, " +
        "firebase_uid, created_at, email_verified, deleted_at, deactivated_at, " +
        "must_change_password, temp_password_expires_at, photo_object_key, stripe_customer_id, fcm_token"

@Component
class UserRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: UserRowMapper,
) : UserRepository {

    override fun existsByEmail(email: Email): Boolean {
        val sql = "SELECT COUNT(*) FROM users WHERE email = :email"
        val count = jdbc.queryForObject(sql, mapOf("email" to email.value), Int::class.java) ?: 0
        return count > 0
    }

    override fun save(user: User): User {
        val sql =
            """
            INSERT INTO users (id, email, password_hash, first_name, last_name,
                                phone_number, birth_date, role, firebase_uid, created_at,
                                email_verified, deactivated_at, must_change_password, temp_password_expires_at)
            VALUES (:id, :email, :passwordHash, :firstName, :lastName,
                    :phoneNumber, :birthDate, :role, :firebaseUid, :createdAt,
                    :emailVerified, :deactivatedAt, :mustChangePassword, :tempPasswordExpiresAt)
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", user.id.value)
                .addValue("email", user.email.value)
                .addValue("passwordHash", user.hashedPassword.value)
                .addValue("firstName", user.firstName)
                .addValue("lastName", user.lastName)
                .addValue("phoneNumber", user.phoneNumber.value)
                .addValue("birthDate", Date.valueOf(user.birthDate))
                .addValue("role", user.role.name)
                .addValue("firebaseUid", user.firebaseUid)
                .addValue("createdAt", Timestamp.from(user.createdAt))
                .addValue("emailVerified", user.emailVerified)
                .addValue("deactivatedAt", user.deactivatedAt?.let { Timestamp.from(it) })
                .addValue("mustChangePassword", user.mustChangePassword)
                .addValue("tempPasswordExpiresAt", user.tempPasswordExpiresAt?.let { Timestamp.from(it) }),
        )
        return user
    }

    override fun update(user: User): User {
        val sql =
            """
            UPDATE users SET
                email                    = :email,
                first_name               = :firstName,
                last_name                = :lastName,
                phone_number             = :phoneNumber,
                birth_date               = :birthDate,
                email_verified           = :emailVerified,
                deactivated_at           = :deactivatedAt,
                must_change_password     = :mustChangePassword,
                temp_password_expires_at = :tempPasswordExpiresAt
            WHERE id = :id
              AND deleted_at IS NULL
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", user.id.value)
                .addValue("email", user.email.value)
                .addValue("firstName", user.firstName)
                .addValue("lastName", user.lastName)
                .addValue("phoneNumber", user.phoneNumber.value)
                .addValue("birthDate", Date.valueOf(user.birthDate))
                .addValue("emailVerified", user.emailVerified)
                .addValue("deactivatedAt", user.deactivatedAt?.let { Timestamp.from(it) })
                .addValue("mustChangePassword", user.mustChangePassword)
                .addValue("tempPasswordExpiresAt", user.tempPasswordExpiresAt?.let { Timestamp.from(it) }),
        )
        return user
    }

    override fun findByEmail(email: Email): User? {
        val sql =
            """
            SELECT $USER_COLUMNS
            FROM users
            WHERE email = :email
            """.trimIndent()
        return jdbc.query(sql, mapOf("email" to email.value), rowMapper).firstOrNull()
    }

    override fun findByPhoneNumber(phoneNumber: PhoneNumber): User? {
        val sql =
            """
            SELECT $USER_COLUMNS
            FROM users
            WHERE phone_number = :phoneNumber
            """.trimIndent()
        return jdbc.query(sql, mapOf("phoneNumber" to phoneNumber.value), rowMapper).firstOrNull()
    }

    override fun findById(id: UserId): User? {
        val sql =
            """
            SELECT $USER_COLUMNS
            FROM users
            WHERE id = :id
              AND deleted_at IS NULL
            """.trimIndent()
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun findAll(page: Int, size: Int): List<User> {
        val sql =
            """
            SELECT $USER_COLUMNS
            FROM users
            WHERE deleted_at IS NULL
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """.trimIndent()
        return jdbc.query(
            sql,
            mapOf("limit" to size, "offset" to page * size),
            rowMapper,
        )
    }

    override fun count(): Long {
        val sql = "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL"
        return jdbc.queryForObject(sql, emptyMap<String, Any>(), Long::class.java) ?: 0
    }

    override fun markEmailVerified(id: UserId) {
        val sql = "UPDATE users SET email_verified = true WHERE id = :id"
        jdbc.update(sql, mapOf("id" to id.value))
    }

    override fun updatePhotoKey(id: UserId, photoKey: String?) {
        val sql = "UPDATE users SET photo_object_key = :photoKey WHERE id = :id AND deleted_at IS NULL"
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", id.value)
                .addValue("photoKey", photoKey),
        )
    }

    override fun updatePassword(id: UserId, hashedPassword: HashedPassword) {
        val sql =
            """
            UPDATE users SET
                password_hash            = :passwordHash,
                must_change_password     = false,
                temp_password_expires_at = NULL
            WHERE id = :id
            """.trimIndent()
        jdbc.update(sql, mapOf("id" to id.value, "passwordHash" to hashedPassword.value))
    }

    override fun applyReinvitation(id: UserId, hashedPassword: HashedPassword, tempPasswordExpiresAt: Instant) {
        val sql =
            """
            UPDATE users SET
                password_hash            = :passwordHash,
                must_change_password     = true,
                temp_password_expires_at = :tempPasswordExpiresAt
            WHERE id = :id
              AND deleted_at IS NULL
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", id.value)
                .addValue("passwordHash", hashedPassword.value)
                .addValue("tempPasswordExpiresAt", Timestamp.from(tempPasswordExpiresAt)),
        )
    }

    override fun updateStripeCustomerId(id: UserId, stripeCustomerId: String) {
        // Donnée sensible : jamais loguée (cf. règles SQL du CLAUDE.md).
        val sql = "UPDATE users SET stripe_customer_id = :stripeCustomerId WHERE id = :id AND deleted_at IS NULL"
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", id.value)
                .addValue("stripeCustomerId", stripeCustomerId),
        )
    }

    override fun anonymize(id: UserId) {
        val sql =
            """
            UPDATE users SET
                email          = :anonymizedEmail,
                password_hash  = 'DELETED',
                first_name     = 'Compte',
                last_name      = 'Supprimé',
                phone_number   = '0000000000',
                birth_date     = '1970-01-01',
                firebase_uid     = :anonymizedFirebaseUid,
                deleted_at       = NOW(),
                email_verified   = false,
                photo_object_key = NULL,
                stripe_customer_id = NULL
            WHERE id = :id
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", id.value)
                .addValue("anonymizedEmail", "deleted_${id.value}@kara.deleted")
                .addValue("anonymizedFirebaseUid", "DELETED_${id.value}"),
        )
    }
}
