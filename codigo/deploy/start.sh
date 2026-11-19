#!/usr/bin/env bash
# Sobe a API e o Nginx no mesmo container e encerra os dois juntos.
set -euo pipefail

java -XX:MaxRAMPercentage=70.0 -jar /app/app.jar &
api_pid=$!

nginx -g 'daemon off;' &
web_pid=$!

terminate() {
    kill "$api_pid" "$web_pid" 2>/dev/null || true
}
trap terminate TERM INT

status=0
wait -n "$api_pid" "$web_pid" || status=$?

terminate
wait 2>/dev/null || true
exit "$status"
