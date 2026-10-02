package ca.arzook.server.service

import ca.arzook.server.model.MessageType
import ca.arzook.server.model.TradeAction
import ca.arzook.server.model.WebSocketMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage

// ─────────────────────────────────────────────────────────────────────────────
// WebSocketBroadcastService — THE service the backend team calls to push
// messages to the KMP apps.
//
// Usage examples:
//
//   // Push trade update to ALL connected users (public trade list changed)
//   broadcastService.broadcastTradeUpdate("trade-123", TradeAction.CREATED)
//
//   // Push draft created to ONE specific user
//   broadcastService.sendDraftCreated(userId = "user-abc", draftId = "draft-xyz", action = TradeAction.BUYING)
//
//   // Push a notification to one user
//   broadcastService.sendNotification(userId, "Your trade was deposited", "The seller confirmed deposit.")
// ─────────────────────────────────────────────────────────────────────────────
@Service
class WebSocketBroadcastService(
    private val sessionService: WebSocketSessionService,
    private val objectMapper: ObjectMapper
) {

    // ── 1. TRADE_UPDATE — broadcast to ALL users ──────────────────────────────
    // Called when any public trade is created, updated, or deleted.
    fun broadcastTradeUpdate(tradeId: String, action: String) {
        val message = WebSocketMessage(
            type    = MessageType.TRADE_UPDATE,
            tradeId = tradeId,
            action  = action   // CREATED | UPDATED | DELETED
        )
        sendToAll(message)
        println("[Broadcast] TRADE_UPDATE tradeId=$tradeId action=$action → ${sessionService.sessionCount()} clients")
    }

    // ── 2. DRAFT_CREATED — send to specific user ──────────────────────────────
    fun sendDraftCreated(userId: String, draftId: String, action: String) {
        val message = WebSocketMessage(
            type    = MessageType.DRAFT_CREATED,
            tradeId = draftId,
            action  = action   // BUYING | SELLING
        )
        sendToUser(userId, message)
    }

    // ── 3. DRAFT_UPDATED — send to specific user ──────────────────────────────
    fun sendDraftUpdated(userId: String, draftId: String, action: String) {
        val message = WebSocketMessage(
            type    = MessageType.DRAFT_UPDATED,
            tradeId = draftId,
            action  = action
        )
        sendToUser(userId, message)
    }

    // ── 4. DRAFT_DELETED — send to specific user ──────────────────────────────
    fun sendDraftDeleted(userId: String, draftId: String, action: String) {
        val message = WebSocketMessage(
            type    = MessageType.DRAFT_DELETED,
            tradeId = draftId,
            action  = action
        )
        sendToUser(userId, message)
    }

    // ── 5. TRADE_MATCHED — send to specific user ──────────────────────────────
    fun sendTradeMatched(userId: String, tradeId: String) {
        val message = WebSocketMessage(
            type    = MessageType.TRADE_MATCHED,
            tradeId = tradeId,
            message = "Your trade has been matched!"
        )
        sendToUser(userId, message)
    }

    // ── 6. NOTIFICATION — send to specific user ───────────────────────────────
    fun sendNotification(userId: String, title: String, body: String, data: String? = null) {
        val message = WebSocketMessage(
            type    = MessageType.NOTIFICATION,
            title   = title,
            message = body,
            data    = data
        )
        sendToUser(userId, message)
    }

    // ── 7. CHAT_MESSAGE — send to specific user ───────────────────────────────
    fun sendChatMessage(userId: String, chatMessageJson: String) {
        val message = WebSocketMessage(
            type = MessageType.CHAT_MESSAGE,
            data = chatMessageJson
        )
        sendToUser(userId, message)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun sendToUser(userId: String, message: WebSocketMessage) {
        val json = objectMapper.writeValueAsString(message)
        val sessions = sessionService.getSessionsForUser(userId)
        if (sessions.isEmpty()) {
            println("[Broadcast] User $userId not connected — message not delivered: ${message.type}")
            return
        }
        sessions.forEach { session ->
            try {
                session.sendMessage(TextMessage(json))
                println("[Broadcast] → userId=$userId type=${message.type}")
            } catch (e: Exception) {
                println("[Broadcast] Failed to send to userId=$userId: ${e.message}")
            }
        }
    }

    private fun sendToAll(message: WebSocketMessage) {
        val json = objectMapper.writeValueAsString(message)
        sessionService.getAllSessions().forEach { session ->
            try {
                session.sendMessage(TextMessage(json))
            } catch (e: Exception) {
                println("[Broadcast] Failed to send to session ${session.id}: ${e.message}")
            }
        }
    }
}
