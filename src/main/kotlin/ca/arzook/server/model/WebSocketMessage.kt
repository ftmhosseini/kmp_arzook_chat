package ca.arzook.server.model

import com.fasterxml.jackson.annotation.JsonInclude

// ─────────────────────────────────────────────────────────────────────────────
// WebSocketMessage — matches exactly what WebSocketManager.kt in the KMP apps
// expects to receive. All fields optional except "type".
//
// JsonInclude NON_NULL → fields that are null are NOT included in the JSON,
// keeping messages lean.
// ─────────────────────────────────────────────────────────────────────────────
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WebSocketMessage(
    val type: String,
    val data: String? = null,
    val tradeId: String? = null,
    val action: String? = null,
    val title: String? = null,
    val message: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Message type constants — use these everywhere to avoid typos
// ─────────────────────────────────────────────────────────────────────────────
object MessageType {
    const val TRADE_UPDATE    = "TRADE_UPDATE"
    const val DRAFT_CREATED   = "DRAFT_CREATED"
    const val DRAFT_UPDATED   = "DRAFT_UPDATED"
    const val DRAFT_DELETED   = "DRAFT_DELETED"
    const val TRADE_MATCHED   = "TRADE_MATCHED"
    const val NOTIFICATION    = "NOTIFICATION"
    const val CHAT_MESSAGE    = "CHAT_MESSAGE"
    const val PING            = "PING"
    const val PONG            = "PONG"
}

// ─────────────────────────────────────────────────────────────────────────────
// Action constants — used in trade/draft messages
// ─────────────────────────────────────────────────────────────────────────────
object TradeAction {
    const val CREATED  = "CREATED"
    const val UPDATED  = "UPDATED"
    const val DELETED  = "DELETED"
    const val BUYING   = "BUYING"
    const val SELLING  = "SELLING"
}

// ─────────────────────────────────────────────────────────────────────────────
// Represents a connected client session with its userId extracted from JWT
// ─────────────────────────────────────────────────────────────────────────────
data class ConnectedSession(
    val sessionId: String,
    val userId: String
)
