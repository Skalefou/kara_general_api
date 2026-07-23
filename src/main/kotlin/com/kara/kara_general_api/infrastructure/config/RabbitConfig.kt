package com.kara.kara_general_api.infrastructure.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import tools.jackson.databind.json.JsonMapper
import java.net.URI

/**
 * Topologie et sérialisation RabbitMQ pour le traitement d'image asynchrone.
 *
 * - `image-jobs` (API → worker) : la queue est déclarée avec un dead-letter-exchange `image-jobs.dlx` de sorte
 *   qu'un message rejeté sans requeue (poison) parte vers `image-jobs.dlq` au lieu de boucler.
 * - `image-results` (worker → API) : consommée par [com.kara.kara_general_api.infrastructure.adapter.input.messaging.imagejob.ImageResultListener].
 * - Le convertisseur JSON est basé sur Jackson 3 (`tools.jackson`, déjà utilisé dans ce repo) et réutilise le
 *   [JsonMapper] configuré par Spring Boot → champs en camelCase, cohérents avec le contrat figé du worker.
 *
 * `@Profile("!test")` : en test on ne se connecte pas au broker (cf. `application.properties` de test qui coupe
 * l'auto-déclaration et le démarrage des listeners).
 */
@Configuration
@Profile("!test")
class RabbitConfig {

    companion object {
        const val JOBS_QUEUE = "image-jobs"
        const val RESULTS_QUEUE = "image-results"
        const val DLX_EXCHANGE = "image-jobs.dlx"
        const val DLQ_QUEUE = "image-jobs.dlq"
    }

    @Bean
    fun rabbitConnectionFactory(
        @Value("\${RABBITMQ_URL}") rabbitmqUrl: String,
    ): ConnectionFactory = CachingConnectionFactory(URI(rabbitmqUrl))

    @Bean
    fun messageConverter(jsonMapper: JsonMapper): MessageConverter = JacksonJsonMessageConverter(jsonMapper)

    @Bean
    fun imageJobsQueue(): Queue =
        QueueBuilder.durable(JOBS_QUEUE)
            .deadLetterExchange(DLX_EXCHANGE)
            .deadLetterRoutingKey(JOBS_QUEUE)
            .build()

    @Bean
    fun imageResultsQueue(): Queue = QueueBuilder.durable(RESULTS_QUEUE).build()

    @Bean
    fun imageJobsDlx(): DirectExchange = DirectExchange(DLX_EXCHANGE, true, false)

    @Bean
    fun imageJobsDlq(): Queue = QueueBuilder.durable(DLQ_QUEUE).build()

    @Bean
    fun imageJobsDlqBinding(): Binding =
        BindingBuilder.bind(imageJobsDlq()).to(imageJobsDlx()).with(JOBS_QUEUE)
}
