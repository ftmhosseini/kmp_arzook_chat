package ca.arzook.server.config

import ca.arzook.server.model.TradeAction
import ca.arzook.server.service.WebSocketBroadcastService
import ca.arzook.server.service.WebSocketSessionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ─────────────────────────────────────────────────────────────────────────────
// BroadcastController — REST API that lets other backend services
// trigger WebSocket pushes to the KMP apps.
//
// The real backend (Java/Go/Python) calls these endpoints to push
// notifications when something changes in the database.
//
// Example:
//   POST /api/broadcast/trade-update
//   { "tradeId": "abc-123", "action": "CREATED" }
//   → All connected clients receive TRADE_UPDATE message instantly
// ─────────────────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/broadcast")
class BroadcastController(
    private val broadcastService: WebSocketBroadcastService,
    private val sessionService: WebSocketSessionService
) {

    // GET /api/broadcast/status — check how many clients are connected
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "running",
            "connectedClients" to sessionService.sessionCount()
        ))
    }

    // POST /api/broadcast/trade-update
    @PostMapping("/trade-update")
    fun tradeUpdate(@RequestBody body: TradeUpdateRequest): ResponseEntity<String> {
        broadcastService.broadcastTradeUpdate(body.tradeId, body.action)
        return ResponseEntity.ok("Trade update broadcast sent")
    }

    // POST /api/broadcast/draft
    @PostMapping("/draft")
    fun draftEvent(@RequestBody body: DraftEventRequest): ResponseEntity<String> {
        when (body.eventType) {
            "DRAFT_CREATED" -> broadcastService.sendDraftCreated(body.userId, body.draftId, body.action)
            "DRAFT_UPDATED" -> broadcastService.sendDraftUpdated(body.userId, body.draftId, body.action)
            "DRAFT_DELETED" -> broadcastService.sendDraftDeleted(body.userId, body.draftId, body.action)
            else -> return ResponseEntity.badRequest().body("Unknown eventType: ${body.eventType}")
        }
        return ResponseEntity.ok("Draft event sent to user ${body.userId}")
    }

    // POST /api/broadcast/trade-matched
    @PostMapping("/trade-matched")
    fun tradeMatched(@RequestBody body: TradeMatchedRequest): ResponseEntity<String> {
        broadcastService.sendTradeMatched(body.userId, body.tradeId)
        return ResponseEntity.ok("Trade matched sent to user ${body.userId}")
    }

    // POST /api/broadcast/notification
    @PostMapping("/notification")
    fun notification(@RequestBody body: NotificationRequest): ResponseEntity<String> {
        broadcastService.sendNotification(body.userId, body.title, body.message, body.data)
        return ResponseEntity.ok("Notification sent to user ${body.userId}")
    }

    // POST /api/broadcast/chat
    @PostMapping("/chat")
    fun chat(@RequestBody body: ChatRequest): ResponseEntity<String> {
        broadcastService.sendChatMessage(body.userId, body.chatMessageJson)
        return ResponseEntity.ok("Chat message sent to user ${body.userId}")
    }
}

// ── Request models ─────────────────────────────────────────────────────────

data class TradeUpdateRequest(val tradeId: String, val action: String)

data class DraftEventRequest(
    val eventType: String,   // DRAFT_CREATED | DRAFT_UPDATED | DRAFT_DELETED
    val userId: String,
    val draftId: String,
    val action: String       // BUYING | SELLING
)

data class TradeMatchedRequest(val userId: String, val tradeId: String)

data class NotificationRequest(
    val userId: String,
    val title: String,
    val message: String,
    val data: String? = null
)

data class ChatRequest(val userId: String, val chatMessageJson: String)
