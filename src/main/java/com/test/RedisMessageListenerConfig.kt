package com.test

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor


/**
 * @author theo
 * @date 2019/5/17
 */
internal val FEATURE_INSTALLED_LIST = AttributeKey<Attributes>("ApplicationFeatureRegistry")

@Configuration
open class RedisMessageListenerConfig(private val redisTemplate: RedisTemplate<Any, Any>, private val applicationEngine: ApplicationEngine) {
    @Bean
    open fun redisMessageListenerContainer(): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.connectionFactory = redisTemplate.connectionFactory
        container.setTaskExecutor(ConcurrentTaskExecutor())
        val topic = PatternTopic("*")
        container.addMessageListener(MessageListener { message, _ ->
            val body = String(message.body)
            val channel = String(message.channel)
            var value: Any? = null
            val attributes = applicationEngine.application.attributes
            val attributeKey = attributes.allKeys.filter { it.name == "ApplicationFeatureRegistry" }[0] as AttributeKey<Attributes>
            val attribute = attributes[attributeKey]
            val routeKey = attribute.allKeys.filter { it.name == "Routing" }[0]  as AttributeKey<Attributes>
            val route:Any = attribute[routeKey]
            if (route is Routing) {
                route.children
            }
            applicationEngine.application.routing {
                children.forEach {
                    it.
                }
                get(body) {
                    call.respond("channel -> $channel \tbody -> $body")
                }
            }
        }, topic)
        return container
    }


}