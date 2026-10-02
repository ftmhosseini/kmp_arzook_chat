package ca.arzook.server.config

import ca.arzook.server.handler.ArzookWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

// ─────────────────────────────────────────────────────────────────────────────
// WebSocketConfig — registers the /ws endpoint
//
// The KMP app connects to:
//   wss://api.arzook.ca/ws          (production)
//   ws://10.0.2.2:8080/ws           (Android emulator dev)
//   ws://localhost:8080/ws          (local dev)
// ─────────────────────────────────────────────────────────────────────────────
@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val webSocketHandler: ArzookWebSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(webSocketHandler, "/ws")
            .setAllowedOrigins("*")   // In production: restrict to your domain
    }

    // Increase message buffer size for large trade payloads
    @Bean
    fun serverContainer(): ServletServerContainerFactoryBean {
        return ServletServerContainerFactoryBean().apply {
            setMaxTextMessageBufferSize(8192)
            setMaxBinaryMessageBufferSize(8192)
        }
    }
}
