#!/usr/bin/env bash
# End-to-end smoke test against the locally running stack (docker compose up).
# Creates an order and polls until payment processing flips its status.
set -euo pipefail

ORDER_API="${ORDER_API:-http://localhost:8080/api/v1/orders}"
AMOUNT="${1:-42.50}"
CUSTOMER="${2:-customer-123}"

echo "POST ${ORDER_API} (customer=${CUSTOMER} amount=${AMOUNT})"
CREATE_RESP=$(curl -sS -X POST "$ORDER_API" \
  -H 'content-type: application/json' \
  -d "{\"customerId\":\"${CUSTOMER}\",\"amount\":${AMOUNT},\"currency\":\"USD\"}")
echo "Created: ${CREATE_RESP}"

ORDER_ID=$(echo "$CREATE_RESP" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
if [[ -z "$ORDER_ID" ]]; then
  echo "Could not parse order id" >&2
  exit 1
fi

echo "Polling order ${ORDER_ID} for terminal status..."
for i in $(seq 1 20); do
  sleep 1
  STATUS=$(curl -sS "${ORDER_API}/${ORDER_ID}" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')
  echo "  [${i}] status=${STATUS}"
  if [[ "$STATUS" == "PAID" || "$STATUS" == "PAYMENT_FAILED" ]]; then
    echo "Done. Final status: ${STATUS}"
    exit 0
  fi
done

echo "Timed out waiting for payment processing." >&2
exit 1
