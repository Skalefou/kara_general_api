package com.kara.kara_general_api.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** Active le planificateur Spring (tâches @Scheduled : annulation des réservations expirées, rappels…). */
@Configuration
@EnableScheduling
class SchedulerConfig
