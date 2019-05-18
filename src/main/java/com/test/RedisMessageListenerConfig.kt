package com.test

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.routing.get
import io.ktor.server.engine.ApplicationEngine
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import io.ktor.util.pipeline.ContextDsl
import io.ktor.util.pipeline.PipelineInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
import org.w3c.dom.Attr
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField


/**
 * @author theo
 * @date 2019/5/17
 */
internal val internalKey = AttributeKey<String>("ID")

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
            println("channel -> $channel \tbody -> $body")
            var value: Any? = null
            val attributes = applicationEngine.application.attributes
            val attributeKey = attributes.allKeys.filter { it.name == "ApplicationFeatureRegistry" }[0] as AttributeKey<Attributes>
            val attribute = attributes[attributeKey]
            val routeKey = attribute.allKeys.filter { it.name == "Routing" }[0] as AttributeKey<Attributes>
            val route: Any = attribute[routeKey]
            if (route is Routing) {
                val childList = route.childList()
                when {
                    channel.contains("lrem") -> {
                        println("removing $body")
                        childList.forEach {
                           val grandChildList = it.childList()
                            grandChildList.removeIf {r->
                                val key = r.attributes.allKeys.stream().filter { it.name == "ID" }.findAny()
                                if (key.isPresent) {
                                    val realKey = key.get() as AttributeKey<String>
                                    val v = r.attributes.getOrNull(realKey)
                                    println(v)
                                    return@removeIf v == body
                                }
                                false
                            }

                        }
                    }
                    channel.contains("lpush") ->
                        applicationEngine.application.routing {
                            markedRoute(body, HttpMethod.Get, body) {
                                handle {
                                    this.context.attributes.put(internalKey, body)
                                    call.respond("channel -> $channel \tbody -> $body")
                                }
                            }

                        }
                }
            }

        }, topic)
        return container
    }
}


fun Route.childList(): MutableList<Route> {
    val field = Route::class.declaredMemberProperties.stream().filter { it.name == "childList" }.findAny().get().javaField
    field?.isAccessible = true
    return field?.get(this) as MutableList<Route>
}

@ContextDsl
fun Route.markedRoute(path: String, method: HttpMethod, value: String, build: Route.() -> Unit): Route {
    val selector = HttpMethodRouteSelector(method)
    val createRouteFromPath = createRouteFromPath(path)
    val child = createRouteFromPath.createChild(selector)
    child.attributes.put(internalKey, value)
    return child.apply(build)
}