package com.kara.kara_general_api.infrastructure.adapter.output.persistence.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true)
    var email: String,
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,
    @Column(name = "first_name", nullable = false)
    var firstName: String,
    @Column(name = "last_name", nullable = false)
    var lastName: String,
    @Column(name = "phone_number", nullable = false)
    var phoneNumber: String,
    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate,
    @Column(nullable = false)
    var role: String,
    @Column(name = "firebase_uid", nullable = false, unique = true)
    var firebaseUid: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,
)
