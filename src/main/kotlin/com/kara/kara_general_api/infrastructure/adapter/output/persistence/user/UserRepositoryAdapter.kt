package com.kara.kara_general_api.infrastructure.adapter.output.persistence.user

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Date
import java.sql.Timestamp

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
                                phone_number, birth_date, role, firebase_uid, created_at, email_verified)
            VALUES (:id, :email, :passwordHash, :firstName, :lastName,
                    :phoneNumber, :birthDate, :role, :firebaseUid, :createdAt, :emailVerified)
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
                .addValue("emailVerified", user.emailVerified),
        )
        return user
    }

    override fun findByEmail(email: Email): User? {
        val sql =
            """
            SELECT id, email, password_hash, first_name, last_name, phone_number,
                   birth_date, role, firebase_uid, created_at, email_verified
            FROM users
            WHERE email = :email
            """.trimIndent()
        return jdbc.query(sql, mapOf("email" to email.value), rowMapper).firstOrNull()
    }

    override fun findById(id: UserId): User? {
        val sql =
            """
            SELECT id, email, password_hash, first_name, last_name, phone_number,
                   birth_date, role, firebase_uid, created_at, email_verified
            FROM users
            WHERE id = :id
              AND deleted_at IS NULL
            """.trimIndent()
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun markEmailVerified(id: UserId) {
        val sql = "UPDATE users SET email_verified = true WHERE id = :id"
        jdbc.update(sql, mapOf("id" to id.value))
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
                firebase_uid   = :anonymizedFirebaseUid,
                deleted_at     = NOW(),
                email_verified = false
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
