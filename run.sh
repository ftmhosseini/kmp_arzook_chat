#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# run.sh — starts the Arzook WebSocket server
# Usage: ./run.sh
# ─────────────────────────────────────────────────────────────────────────────

echo "🚀 Starting Arzook WebSocket Server..."
echo "   WebSocket endpoint: ws://localhost:8080/ws"
echo "   Status endpoint:    http://localhost:8080/api/broadcast/status"
echo ""

./gradlew bootRun
