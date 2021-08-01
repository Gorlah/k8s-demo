package io.erichaag.k8s.demo

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import java.util.UUID

@SpringBootApplication
class K8sDemoApplication {

    @Bean
    fun router(messageHandler: MessageHandler): RouterFunction<ServerResponse> {
        return router {
            POST("/messages") { messageHandler.createMessage(it) }
            GET("/messages") { messageHandler.getMessages() }
            GET("/messages/{messageId}") { messageHandler.getMessage(it) }
        }
    }

    @Component
    class MessageHandler(private val messageRepository: MessageRepository) {

        private val log = LoggerFactory.getLogger(MessageHandler::class.java)

        fun createMessage(request: ServerRequest): Mono<ServerResponse> {
            return request
                .bodyToMono<MessageRequest>()
                .doOnNext { log.info("Processing message: $it") }
                .map { Message(id = UUID.randomUUID(), message = it.message) }
                .flatMap { messageRepository.save(it) }
                .doOnNext { log.info("(Success) Processing message: $it") }
                .doOnError { log.info("(Fail) Processing message: $it") }
                .flatMap { ok().bodyValue(it) }
        }

        fun getMessages(): Mono<ServerResponse> {
            return messageRepository.findAll().collectList()
                .doOnNext { log.info("(Success) Fetching messages") }
                .doOnError { log.info("(Fail) Fetching messages") }
                .flatMap { ok().bodyValue(it) }
        }

        fun getMessage(request: ServerRequest): Mono<ServerResponse> {
            return Mono.just(request)
                .map { it.pathVariable("messageId") }
                .map { UUID.fromString(it) }
                .doOnNext { log.info("Fetching message: $it") }
                .flatMap { messageRepository.findById(it) }
                .doOnNext { log.info("(Success) Fetching message: $it") }
                .doOnError { log.info("(Fail) Fetching message: $it") }
                .flatMap { ok().bodyValue(it) }
        }
    }

    @Bean
    fun mongoConfigurer(mappingMongoConverter: MappingMongoConverter) = InitializingBean {
        mappingMongoConverter.setTypeMapper(DefaultMongoTypeMapper(null))
    }
}

interface MessageRepository : ReactiveMongoRepository<Message, UUID>

fun main(args: Array<String>) {
    runApplication<K8sDemoApplication>(*args)
}

data class MessageRequest(
    val message: String,
)

@Document
data class Message(
    val id: UUID,
    val message: String,
)