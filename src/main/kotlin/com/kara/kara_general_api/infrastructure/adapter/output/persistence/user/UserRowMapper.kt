package com.kara.kara_general_api.infrastructure.adapter.output.persistence.user

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class UserRowMapper : RowMapper<User> {
    override fun mapRow(rs: ResultSet, rowNum: Int): User =
        User(
            id = UserId(rs.getObject("id", UUID::class.java)),
            email = Email(rs.getString("email")),
            hashedPassword = HashedPassword(rs.getString("password_hash")),
            firstName = rs.getString("first_name"),
            lastName = rs.getString("last_name"),
            phoneNumber = PhoneNumber(rs.getString("phone_number")),
            birthDate = rs.getDate("birth_date").toLocalDate(),
            role = UserRole.valueOf(rs.getString("role")),
            firebaseUid = rs.getString("firebase_uid"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            emailVerified = rs.getBoolean("email_verified"),
            deletedAt = rs.getTimestamp("deleted_at")?.toInstant(),
            deactivatedAt = rs.getTimestamp("deactivated_at")?.toInstant(),
            mustChangePassword = rs.getBoolean("must_change_password"),
            tempPasswordExpiresAt = rs.getTimestamp("temp_password_expires_at")?.toInstant(),
            photoKey = rs.getString("photo_object_key"),
        )
}
