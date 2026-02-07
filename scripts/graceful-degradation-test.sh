#!/bin/bash
# Test: Stop Jaeger -> verify services still respond normally
#
# This test proves that the OTel Java Agent degrades gracefully:
# when the tracing backend (Jaeger) is unavailable, application
# functionality is completely unaffected.
#
# Prerequisites: docker-compose up -d

echo "=== Graceful Degradation Test ==="
echo "Step 1: Verify services are working with Jaeger"
echo "Step 2: Stop Jaeger"
echo "Step 3: Send requests and verify services still work"
echo ""

REQUEST_BODY='{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'

# Test with Jaeger running
echo "--- Test with Jaeger running ---"
response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8081/api/orders" \
    -H "Content-Type: application/json" \
    -d "$REQUEST_BODY")
echo "Response code (with Jaeger): $response"

if [ "$response" != "200" ]; then
    echo "ERROR: Service not responding with Jaeger running. Aborting test."
    exit 1
fi

# Stop Jaeger
echo ""
echo "--- Stopping Jaeger ---"
docker-compose stop jaeger
echo "Jaeger stopped. Waiting 5 seconds for agent to detect disconnection..."
sleep 5

# Test without Jaeger
echo ""
echo "--- Test without Jaeger (should still work) ---"
response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8081/api/orders" \
    -H "Content-Type: application/json" \
    -d "$REQUEST_BODY")
echo "Response code (without Jaeger): $response"

if [ "$response" = "200" ]; then
    echo ""
    echo "PASS: Services work normally without Jaeger (graceful degradation verified)"
else
    echo ""
    echo "FAIL: Services failed without Jaeger (response code: $response)"
fi

# Restart Jaeger
echo ""
echo "--- Restarting Jaeger ---"
docker-compose start jaeger
echo "Jaeger restarted."
echo ""
echo "=== Graceful Degradation Test Complete ==="
