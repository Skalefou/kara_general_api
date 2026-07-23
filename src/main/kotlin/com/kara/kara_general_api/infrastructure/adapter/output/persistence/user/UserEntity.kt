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
    @Column(nullable = false, unique = true, columnDefinition = "varchar(255)")
    var email: String,
    @Column(name = "password_hash", nullable = false, columnDefinition = "varchar(255)")
    var passwordHash: String,
    @Column(name = "first_name", nullable = false, columnDefinition = "varchar(100)")
    var firstName: String,
    @Column(name = "last_name", nullable = false, columnDefinition = "varchar(100)")
    var lastName: String,
    @Column(name = "phone_number", nullable = false, columnDefinition = "varchar(20)")
    var phoneNumber: String,
    @Column(name = "birth_date", nullable = false, columnDefinition = "date")
    var birthDate: LocalDate,
    @Column(nullable = false, columnDefinition = "varchar(50)")
    var role: String,
    @Column(name = "firebase_uid", nullable = false, unique = true, columnDefinition = "varchar(128)")
    var firebaseUid: String,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean")
    var emailVerified: Boolean = false,
    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    var deletedAt: Instant? = null,
    @Column(name = "deactivated_at", columnDefinition = "timestamptz")
    var deactivatedAt: Instant? = null,
    @Column(name = "must_change_password", nullable = false, columnDefinition = "boolean default false")
    var mustChangePassword: Boolean = false,
    @Column(name = "temp_password_expires_at", columnDefinition = "timestamptz")
    var tempPasswordExpiresAt: Instant? = null,
    @Column(name = "photo_object_key", columnDefinition = "varchar(512)")
    var photoObjectKey: String? = null,
    // PROCESSING | READY | FAILED ; null tant qu'aucune photo n'a été téléversée.
    @Column(name = "photo_status", columnDefinition = "varchar(20)")
    var photoStatus: String? = null,
    @Column(name = "photo_thumbnail_key", columnDefinition = "varchar(512)")
    var photoThumbnailKey: String? = null,
    @Column(name = "photo_full_key", columnDefinition = "varchar(512)")
    var photoFullKey: String? = null,
    @Column(name = "stripe_customer_id", columnDefinition = "varchar(255)")
    var stripeCustomerId: String? = null,
    @Column(name = "fcm_token", columnDefinition = "varchar(512)")
    var fcmToken: String? = null,
)
