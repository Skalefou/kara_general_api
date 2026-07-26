package com.kara.kara_general_api.infrastructure.adapter.output.persistence.scheduler

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "shedlock")
class ShedlockEntity(
    @Id
    @Column(name = "name", nullable = false, columnDefinition = "varchar(64)")
    var name: String = "",
    @Column(name = "lock_until", nullable = false, columnDefinition = "timestamp")
    var lockUntil: LocalDateTime = LocalDateTime.MIN,
    @Column(name = "locked_at", nullable = false, columnDefinition = "timestamp")
    var lockedAt: LocalDateTime = LocalDateTime.MIN,
    @Column(name = "locked_by", nullable = false, columnDefinition = "varchar(255)")
    var lockedBy: String = "",
)
