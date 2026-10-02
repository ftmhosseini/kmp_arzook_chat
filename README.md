# Arzook WebSocket Server

Spring Boot WebSocket server that handles real-time connections from the Arzook KMP mobile apps (Android + iOS).

> For the customer app see `KMP forth_version`. For the admin app see `KMP admin`.

---

## Responsibility in the Arzook System

```
┌─────────────────────────────────────────────────────────┐
│                      Arzook System                       │
├─────────────────┬──────────────────┬────────────────────┤
│   User App      │   Admin App      │  WebSocket Server  │
│ (KMP forth_     │  (KMP admin)     │  (this repo)       │
│  version)       │                  │                    │
│  Android + iOS  │  Android + iOS   │  Spring Boot / JVM │
└────────┬────────┴────────┬─────────┴─────────┬──────────┘
         │  wss://         │  wss://            │
         └────────────────►└───────────────────►│
                                                │
                                     POST /api/broadcast/*
                                                │
                                   api.arzook.ca (main backend)
```

This server has two jobs:

1. **Accept WebSocket connections from mobile apps** — validates JWT tokens and stores sessions by `userId`
2. **Receive push instructions from the main backend** via REST — and forward them to the right connected clients

The main backend never connects to clients directly. It calls this server's REST API, and this server does the WebSocket delivery.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9.25 |
| Framework | Spring Boot 3.3.4 |
| WebSocket | Spring WebSocket (STOMP-free, raw handler) |
| JWT | jjwt 0.12.6 |
| JSON | Jackson + jackson-module-kotlin |
| Async | kotlinx-coroutines-core 1.8.1 |
| JVM | Java 21 |
| Build | Gradle 8.8 |

---

## Project Structure

```
src/main/kotlin/ca/arzook/server/
│
├── ArzookWebSocketServerApplication.kt  ← Spring Boot entry point
│
├── config/
│   ├── WebSocketConfig.kt               ← Registers /ws endpoint
│   └── BroadcastController.kt           ← REST API — main backend calls these
│
├── handler/
│   └── ArzookWebSocketHandler.kt        ← Connection lifecycle (connect / message / disconnect)
│
├── service/
│   ├── WebSocketSessionService.kt       ← Stores active sessions by userId
│   └── WebSocketBroadcastService.kt     ← Sends messages to connected clients
│
├── model/
│   └── WebSocketMessage.kt             ← Message structure + type constants
│
└── security/
    └── JwtService.kt                   ← Validates JWT tokens on connect
```

---

## Message Format

All messages to clients use this JSON structure (matches the KMP app exactly):

```json
{
  "type": "MESSAGE_TYPE",
  "data": "optional string",
  "tradeId": "optional",
  "action": "optional",
  "title": "optional",
  "message": "optional"
}
```

### Message Types

| Type | Direction | Who receives it | When |
|------|-----------|-----------------|------|
| `TRADE_UPDATE` | Server → All clients | Everyone | Any public trade changes |
| `DRAFT_CREATED` | Server → User | Specific user | User creates a buying/selling draft |
| `DRAFT_UPDATED` | Server → User | Specific user | User updates their draft |
| `DRAFT_DELETED` | Server → User | Specific user | User deletes their draft |
| `TRADE_MATCHED` | Server → User | Specific user | Trade is matched |
| `NOTIFICATION` | Server → User | Specific user | Any notification |
| `CHAT_MESSAGE` | Server → User | Specific user | Admin sends a chat message |
| `PING` | Client → Server | — | Keep-alive every 20 seconds |
| `PONG` | Server → Client | — | Response to ping |

---

## How to Push Messages from the Main Backend

Call these REST endpoints from your main backend when something changes:

### Trade Update — broadcasts to ALL connected users
```bash
curl -X POST http://localhost:8080/api/broadcast/trade-update \
  -H "Content-Type: application/json" \
  -d '{"tradeId": "abc-123", "action": "CREATED"}'
```
`action` values: `CREATED` | `UPDATED` | `DELETED`

### Draft Event — to a specific user
```bash
curl -X POST http://localhost:8080/api/broadcast/draft \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "DRAFT_CREATED",
    "userId": "user-id-here",
    "draftId": "draft-id-here",
    "action": "BUYING"
  }'
```
`eventType`: `DRAFT_CREATED` | `DRAFT_UPDATED` | `DRAFT_DELETED`
`action`: `BUYING` | `SELLING`

### Trade Matched — to a specific user
```bash
curl -X POST http://localhost:8080/api/broadcast/trade-matched \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-id-here", "tradeId": "trade-id-here"}'
```

### Notification — to a specific user
```bash
curl -X POST http://localhost:8080/api/broadcast/notification \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-id-here",
    "title": "Trade Deposited",
    "message": "Your trade has been deposited.",
    "data": "optional extra data"
  }'
```

### Chat Message — to a specific user
```bash
curl -X POST http://localhost:8080/api/broadcast/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-id-here",
    "chatMessageJson": "{\"id\":\"msg-1\",\"text\":\"Hello\",\"timestamp\":1234567890,\"fromAdmin\":true,\"read\":false}"
  }'
```

### Status Check
```bash
curl http://localhost:8080/api/broadcast/status
# → {"status":"running","connectedClients":0}
```

---

## Local Setup

### Prerequisites

- JDK 21+ — `java -version` to check
- Gradle (or use the `./gradlew` wrapper)

### 1. Clone the repo

```bash
git clone <repo-url>
cd arzook-websocket-server
```

### 2. Set the JWT secret

The JWT secret must match the secret your main Arzook backend uses to sign tokens.

**For local development**, set it as an environment variable:

```bash
export JWT_SECRET=<your-base64-encoded-secret>
```

Or create a file `src/main/resources/application-local.properties` (gitignored):

```properties
app.jwt.secret=<your-base64-encoded-secret>
```

The secret must be:
- Base64-encoded
- At least 32 bytes (256 bits) when decoded
- The same key used by the main authentication server

### 3. Run the server

```bash
./run.sh

# Or directly:
./gradlew bootRun
```

Server starts at `ws://localhost:8080/ws`.

Verify it's running:
```bash
curl http://localhost:8080/api/broadcast/status
# → {"status":"running","connectedClients":0}
```

---

## Testing with wscat

```bash
# Install wscat
npm install -g wscat

# Connect (replace with a real JWT from the mobile app login)
wscat -c ws://localhost:8080/ws -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Send a ping
{"type":"PING"}
# Server responds: {"type":"PONG"}
```

---

## GitHub Actions CI

The workflow at `.github/workflows/ci.yml` runs on every push and pull request to `main`/`master`. It:
1. Runs tests
2. Builds the fat JAR (`bootJar`)
3. Uploads the JAR as a build artifact

### Required GitHub Secret

Add this in your GitHub repo under **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name | Description |
|-------------|-------------|
| `JWT_SECRET` | Base64-encoded JWT secret — must match the main backend |

---

## Deployment

The server is a standard Spring Boot fat JAR. Deploy it anywhere that runs Java 21.

### Build the fat JAR

```bash
./gradlew bootJar
# Output: build/libs/arzook-websocket-server-1.0.0.jar
```

### Run in production

```bash
JWT_SECRET=<your-secret> java -jar build/libs/arzook-websocket-server-1.0.0.jar
```

Or set as a system environment variable and run:

```bash
java -jar build/libs/arzook-websocket-server-1.0.0.jar
```

### Environment Variables (production)

| Variable | Required | Description |
|----------|----------|-------------|
| `JWT_SECRET` | Yes | Base64-encoded JWT secret matching the main backend |
| `SERVER_PORT` | No | Port to listen on (default: `8080`) |

### Deploy alongside the main backend

1. Build the JAR: `./gradlew bootJar`
2. Copy to your server: `scp build/libs/arzook-websocket-server-1.0.0.jar user@server:/app/`
3. Create a systemd service or run with a process manager (PM2, supervisor, etc.)
4. Set `JWT_SECRET` in the service environment
5. Configure your reverse proxy (Nginx) to proxy WebSocket traffic:

```nginx
# In your api.arzook.ca server block
location /ws {
    proxy_pass http://localhost:8080/ws;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
}
```

### Docker (optional)

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/arzook-websocket-server-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t arzook-websocket-server .
docker run -e JWT_SECRET=<your-secret> -p 8080:8080 arzook-websocket-server
```

---

## What the Backend Team Needs to Do

1. **Set the correct JWT secret** — `JWT_SECRET` env var must match the secret the main backend uses to sign user tokens
2. **Call the REST endpoints** from your main backend whenever:
   - A trade is created/updated/deleted → `POST /api/broadcast/trade-update`
   - A user's draft changes → `POST /api/broadcast/draft`
   - A trade is matched → `POST /api/broadcast/trade-matched`
   - Any other notification → `POST /api/broadcast/notification`
   - Admin sends a chat message → `POST /api/broadcast/chat`
3. **Deploy this server** alongside the main backend (same server or separate)
4. **Configure Nginx** to proxy `wss://api.arzook.ca/ws` to this server (see Nginx config above)

---

## Connection Flow

```
Mobile App                  This Server              Main Backend
    │                           │                         │
    │── GET /ws ──────────────► │                         │
    │   Authorization: Bearer   │                         │
    │         <jwt>             │                         │
    │                           │ validate JWT            │
    │                           │ extract userId          │
    │                           │ store session           │
    │ ◄── 101 Switching ─────── │                         │
    │                           │                         │
    │── {"type":"PING"} ──────► │                         │
    │ ◄── {"type":"PONG"} ───── │                         │
    │                           │                         │
    │                           │ ◄── POST /api/broadcast │
    │                           │      /trade-update      │
    │ ◄── {"type":"TRADE_UPDATE"│                         │
    │       "tradeId":"..."}    │                         │
```

---

## Common Issues

| Problem | Fix |
|---------|-----|
| `JWT_SECRET` not set | Server won't start — set the env variable |
| Clients connect but get disconnected immediately | JWT token is invalid or expired — check the secret matches |
| Mobile app connects but receives no messages | Main backend is not calling the REST broadcast endpoints |
| `wss://` connection refused | Check Nginx `Upgrade` and `Connection` headers in proxy config |
| Port already in use | Change `SERVER_PORT` env var or `server.port` in `application.properties` |
