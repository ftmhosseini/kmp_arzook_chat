package ca.arzook.server.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64

// ─────────────────────────────────────────────────────────────────────────────
// JwtService — validates the JWT token sent by the KMP app in the
// Authorization header: "Bearer <token>"
//
// The KMP app sends this in WebSocketManager.kt:
//   header("Authorization", "Bearer $token")
// ─────────────────────────────────────────────────────────────────────────────
@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val jwtSecret: String
) {

    /**
     * Extracts the userId (subject) from a JWT token.
     * Returns null if the token is invalid or expired.
     */
    fun extractUserId(token: String): String? {
        return try {
            val cleanToken = token.removePrefix("Bearer ").trim()
            val key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(cleanToken)
                .payload
            claims.subject
        } catch (e: Exception) {
            println("[JWT] Invalid token: ${e.message}")
            null
        }
    }

    /**
     * Returns true if the token is valid and not expired.
     */
    fun isValid(token: String): Boolean = extractUserId(token) != null
}
