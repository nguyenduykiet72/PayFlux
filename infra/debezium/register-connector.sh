#!/usr/bin/env bash
set -euo pipefail

DEBEZIUM_URL="${DEBEZIUM_URL:-http://localhost:8083}"
CONNECTOR_NAME="outbox-connector"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Registering Debezium connector at ${DEBEZIUM_URL} ..."

if curl -sf "${DEBEZIUM_URL}/connectors/${CONNECTOR_NAME}" >/dev/null 2>&1; then
  echo "Connector ${CONNECTOR_NAME} already exists — deleting before re-register."
  curl -sf -X DELETE "${DEBEZIUM_URL}/connectors/${CONNECTOR_NAME}"
fi

curl -sf -X POST "${DEBEZIUM_URL}/connectors" \
  -H "Content-Type: application/json" \
  -d @"${SCRIPT_DIR}/register-postgres.json"

echo ""
echo "Connector registered. Status:"
curl -sf "${DEBEZIUM_URL}/connectors/${CONNECTOR_NAME}/status" | python3 -m json.tool 2>/dev/null || true
