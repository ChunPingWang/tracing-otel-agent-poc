#!/bin/bash
# Measure JVM startup time with and without agent
#
# Usage:
#   1. Start with agent:    docker-compose up -d && ./scripts/startup-benchmark.sh
#   2. Start without agent: docker-compose -f docker-compose.no-agent.yml up -d && ./scripts/startup-benchmark.sh

echo "=== Startup Time Benchmark ==="
echo "Compare startup time with/without OTel Agent"
echo ""
echo "Usage:"
echo "  1. Start with agent:    docker-compose up -d && ./scripts/startup-benchmark.sh"
echo "  2. Start without agent: docker-compose -f docker-compose.no-agent.yml up -d && ./scripts/startup-benchmark.sh"
echo ""

SERVICES=("order-service:8081" "product-service:8082" "inventory-service:8083" "payment-service:8084" "notification-service:8085")

for entry in "${SERVICES[@]}"; do
    SERVICE="${entry%%:*}"
    PORT="${entry##*:}"
    echo -n "Waiting for $SERVICE (port $PORT)... "
    start_time=$(date +%s%N)
    for i in $(seq 1 120); do
        if curl -s "http://localhost:$PORT/actuator/health" > /dev/null 2>&1 || \
           curl -s "http://localhost:$PORT/h2-console" > /dev/null 2>&1; then
            end_time=$(date +%s%N)
            elapsed=$(echo "scale=2; ($end_time - $start_time) / 1000000000" | bc)
            echo "ready in ${elapsed}s"
            break
        fi
        if [ "$i" -eq 120 ]; then
            echo "TIMEOUT (120s)"
        fi
        sleep 1
    done
done

echo ""
echo "=== Startup Benchmark Complete ==="
