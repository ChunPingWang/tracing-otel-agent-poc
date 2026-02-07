#!/usr/bin/env bash
# Sends an order request to order-service every 10 seconds
# Usage: ./scripts/load-generator.sh
# Stop with Ctrl+C

ORDER_URL="http://localhost:8081/api/orders"
INTERVAL=10
COUNT=0

products=("P001" "P002" "P003")

echo "Starting load generator — POST $ORDER_URL every ${INTERVAL}s"
echo "Press Ctrl+C to stop"
echo "---"

while true; do
  COUNT=$((COUNT + 1))
  PRODUCT=${products[$((RANDOM % ${#products[@]}))]}
  QTY=$(( (RANDOM % 3) + 1 ))

  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$ORDER_URL" \
    -H "Content-Type: application/json" \
    -d "{\"customerId\":\"C001\",\"items\":[{\"productId\":\"$PRODUCT\",\"quantity\":$QTY}]}")

  HTTP_CODE=$(echo "$RESPONSE" | tail -1)
  BODY=$(echo "$RESPONSE" | sed '$d')
  TIMESTAMP=$(date '+%H:%M:%S')

  echo "[$TIMESTAMP] #$COUNT  HTTP $HTTP_CODE  $BODY"

  sleep "$INTERVAL"
done
