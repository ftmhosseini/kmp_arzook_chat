package ca.arzook.server.service

import ca.arzook.server.model.ConnectedSession
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

// ─────────────────────────────────────────────────────────────────────────────
// WebSocketSessionService — thread-safe store of all active WebSocket sessions
//
// Two maps for O(1) lookup in both directions:
//   sessionId → WebSocketSession  (to send messages)
//   sessionId → userId            (to know who owns a session)
//   userId    → Set<sessionId>    (one user may have multiple sessions)
// ─────────────────────────────────────────────────────────────────────────────
@Service
class WebSocketSessionService {

    // sessionId → WebSocketSession
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    // sessionId → userId
    private val sessionUserMap = ConcurrentHashMap<String, String>()

    // userId → Set of sessionIds (a user can be connected on Android + iOS)
    private val userSessionMap = ConcurrentHashMap<String, MutableSet<String>>()

    fun addSession(connected: ConnectedSession, session: WebSocketSession) {
        sessions[connected.sessionId] = session
        sessionUserMap[connected.sessionId] = connected.userId
        userSessionMap.getOrPut(connected.userId) { ConcurrentHashMap.newKeySet() }
            .add(connected.sessionId)
    }

    fun removeSession(sessionId: String) {
        val userId = sessionUserMap.remove(sessionId)
        sessions.remove(sessionId)
        if (userId != null) {
            userSessionMap[userId]?.remove(sessionId)
            if (userSessionMap[userId]?.isEmpty() == true) {
                userSessionMap.remove(userId)
            }
        }
    }

    fun getUserId(sessionId: String): String? = sessionUserMap[sessionId]

    fun getSessionsForUser(userId: String): List<WebSocketSession> {
        return userSessionMap[userId]
            ?.mapNotNull { sessions[it] }
            ?.filter { it.isOpen }
            ?: emptyList()
    }

    fun getAllSessions(): List<WebSocketSession> =
        sessions.values.filter { it.isOpen }

    fun sessionCount(): Int = sessions.size
}
