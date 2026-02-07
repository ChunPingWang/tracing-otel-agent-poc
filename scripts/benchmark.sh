#!/bin/bash
# Benchmark script: 100 requests, exclude first 10 warmup
# Compare avg response time with/without agent
#
# Usage:
#   With agent:    docker-compose up -d && ./scripts/benchmark.sh
#   Without agent: docker-compose -f docker-compose.no-agent.yml up -d && ./scripts/benchmark.sh

BASE_URL="${1:-http://localhost:8081}"
TOTAL_REQUESTS=100
WARMUP=10
REQUEST_BODY='{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'

echo "=== E-Commerce Distributed Tracing Benchmark ==="
echo "Target: $BASE_URL/api/orders"
echo "Total requests: $TOTAL_REQUESTS (warmup: $WARMUP)"
echo ""

# Warmup
echo "--- Warmup phase ($WARMUP requests) ---"
for i in $(seq 1 $WARMUP); do
    curl -s -o /dev/null -w "%{time_total}" -X POST "$BASE_URL/api/orders" \
        -H "Content-Type: application/json" -d "$REQUEST_BODY" > /dev/null
done
echo "Warmup complete."
echo ""

# Benchmark
MEASURED=$((TOTAL_REQUESTS - WARMUP))
echo "--- Benchmark phase ($MEASURED requests) ---"
total_time=0
for i in $(seq 1 $MEASURED); do
    time=$(curl -s -o /dev/null -w "%{time_total}" -X POST "$BASE_URL/api/orders" \
        -H "Content-Type: application/json" -d "$REQUEST_BODY")
    total_time=$(echo "$total_time + $time" | bc)
    echo "Request $i: ${time}s"
done

avg=$(echo "scale=4; $total_time / $MEASURED" | bc)
echo ""
echo "=== Results ==="
echo "Average response time: ${avg}s"
echo "Total time: ${total_time}s"
echo "Measured requests: $MEASURED"
