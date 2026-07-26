package com.kara.kara_general_api

import com.kara.kara_general_api.domain.model.image.ImageProcessingJob
import com.kara.kara_general_api.domain.port.output.ImageProcessingPort
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:latest"))

    /**
     * L'adaptateur RabbitMQ réel ([com.kara.kara_general_api.infrastructure.adapter.output.messaging.imagejob.RabbitImageJobPublisher])
     * est @Profile("!test") : en contexte Spring de test on fournit un no-op pour satisfaire l'injection des
     * services d'upload sans broker. Les tests qui vérifient l'enqueue le font en unitaire (MockK), hors Spring.
     */
    @Bean
    fun imageProcessingPort(): ImageProcessingPort =
        object : ImageProcessingPort {
            override fun enqueue(job: ImageProcessingJob) = Unit
        }
}
