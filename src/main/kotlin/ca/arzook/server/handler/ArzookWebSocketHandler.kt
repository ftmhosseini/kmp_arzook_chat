package ca.arzook.server.handler

import ca.arzook.server.model.ConnectedSession
import ca.arzook.server.model.MessageType
import ca.arzook.server.model.WebSocketMessage
import ca.arzook.server.security.JwtService
import ca.arzook.server.service.WebSocketSessionService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

// ─────────────────────────────────────────────────────────────────────────────
// ArzookWebSocketHandler — handles the full lifecycle of each WebSocket
// connection from the KMP apps (Android + iOS).
//
// Lifecycle:
//   1. Client connects with Authorization: Bearer <jwt>
//   2. Server validates token → accepts or closes with 1008 (policy violation)
//   3. Session stored by userId so we can send targeted messages
//   4. Client sends ping → server sends pong to keep connection alive
//   5. On disconnect → session removed
// ─────────────────────────────────────────────────────────────────────────────
@Component
class ArzookWebSocketHandler(
    private val jwtService: JwtService,
    private val sessionService: WebSocketSessionService,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    // ── Connection opened ─────────────────────────────────────────────────────
    override fun afterConnectionEstablished(session: WebSocketSession) {
        val token = session.handshakeHeaders["Authorization"]?.firstOrNull()

        if (token.isNullOrBlank()) {
            println("[WS] No Authorization header — closing connection ${session.id}")
            session.close(CloseStatus(1008, "Missing Authorization header"))
            return
        }

        val userId = jwtService.extractUserId(token)
        if (userId == null) {
            println("[WS] Invalid or expired token — closing connection ${session.id}")
            session.close(CloseStatus(1008, "Invalid or expired token"))
            return
        }

        sessionService.addSession(ConnectedSession(session.id, userId), session)
        println("[WS] ✅ Connected: userId=$userId sessionId=${session.id}")
        println("[WS] Total connected: ${sessionService.sessionCount()}")
    }

    // ── Message received ──────────────────────────────────────────────────────
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val text = message.payload
        println("[WS] Received from ${session.id}: $text")

        try {
            val msg = objectMapper.readValue(text, WebSocketMessage::class.java)
            when (msg.type) {
                MessageType.PING -> {
                    // Respond with pong — keeps the KMP app connection alive
                    val pong = objectMapper.writeValueAsString(
                        WebSocketMessage(type = MessageType.PONG)
                    )
                    session.sendMessage(TextMessage(pong))
                    println("[WS] Pong sent to ${session.id}")
                }
                else -> {
                    println("[WS] Unhandled message type: ${msg.type}")
                }
            }
        } catch (e: Exception) {
            println("[WS] Failed to parse message: ${e.message}")
        }
    }

    // ── Connection closed ─────────────────────────────────────────────────────
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = sessionService.getUserId(session.id)
        sessionService.removeSession(session.id)
        println("[WS] Disconnected: userId=$userId sessionId=${session.id} status=$status")
        println("[WS] Total connected: ${sessionService.sessionCount()}")
    }

    // ── Transport error ───────────────────────────────────────────────────────
    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        println("[WS] Transport error for ${session.id}: ${exception.message}")
        sessionService.removeSession(session.id)
    }
}
