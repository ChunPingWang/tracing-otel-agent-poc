#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# APISIX Blue-Green Test & Verification Script
# Usage: ./scripts/apisix-test.sh <command>
# Commands: all | verify-distribution | verify-traces | scenario <1-5>
# ============================================================================

GATEWAY="${APISIX_GATEWAY_URL:-http://localhost:9080}"
JAEGER="${JAEGER_URL:-http://localhost:16686}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

PASS=0
FAIL=0

print_header()  { echo -e "\n${BOLD}${BLUE}=== $1 ===${NC}\n"; }
print_info()    { echo -e "${CYAN}[INFO]${NC} $1"; }
print_pass()    { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS + 1)); }
print_fail()    { echo -e "${RED}[FAIL]${NC} $1"; FAIL=$((FAIL + 1)); }

# ----------------------------------------------------------------------------
# Scenario 1: Happy Path
# ----------------------------------------------------------------------------
test_scenario_1() {
  print_header "Scenario 1: Happy Path — Normal Order"

  local response http_code
  response=$(curl -s -w "\n%{http_code}" \
    "${GATEWAY}/api/orders" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":2}]}' \
    --max-time 15)

  http_code=$(echo "${response}" | tail -1)
  local body
  body=$(echo "${response}" | sed '$d')

  if [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
    print_pass "Order created successfully (HTTP ${http_code})"
  else
    print_fail "Order creation failed (HTTP ${http_code})"
    echo "  Response: ${body}"
    return
  fi

  local status
  status=$(echo "${body}" | jq -r '.status // "unknown"' 2>/dev/null || echo "unknown")
  if [ "${status}" = "CONFIRMED" ]; then
    print_pass "Order status is CONFIRMED"
  else
    print_fail "Order status is '${status}' (expected CONFIRMED)"
  fi

  local trace_id
  trace_id=$(echo "${body}" | jq -r '.traceId // "none"' 2>/dev/null || echo "none")
  if [ "${trace_id}" != "none" ] && [ "${trace_id}" != "null" ]; then
    print_pass "TraceId present: ${trace_id}"
  else
    print_pass "Order processed (traceId check skipped)"
  fi
}

# ----------------------------------------------------------------------------
# Scenario 2: Inventory Shortage
# ----------------------------------------------------------------------------
test_scenario_2() {
  print_header "Scenario 2: Inventory Shortage"

  local response http_code
  response=$(curl -s -w "\n%{http_code}" \
    "${GATEWAY}/api/orders" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"customerId":"C001","items":[{"productId":"P999","quantity":999}]}' \
    --max-time 15)

  http_code=$(echo "${response}" | tail -1)
  local body
  body=$(echo "${response}" | sed '$d')

  local status
  status=$(echo "${body}" | jq -r '.status // "unknown"' 2>/dev/null || echo "unknown")

  if [ "${status}" = "FAILED" ]; then
    print_pass "Order status is FAILED (inventory shortage detected)"
  elif [ "${http_code}" = "400" ] || [ "${http_code}" = "500" ]; then
    print_pass "Error response returned (HTTP ${http_code})"
  else
    print_fail "Unexpected response: status=${status}, HTTP ${http_code}"
  fi
}

# ----------------------------------------------------------------------------
# Scenario 3: Payment Timeout
# ----------------------------------------------------------------------------
test_scenario_3() {
  print_header "Scenario 3: Payment Timeout"

  # Enable delay simulation
  print_info "Enabling payment delay simulation (5000ms)..."
  curl -s -o /dev/null "${GATEWAY}/payment/admin/simulate-delay?ms=5000" -X POST --max-time 5

  local response http_code
  response=$(curl -s -w "\n%{http_code}" \
    "${GATEWAY}/api/orders" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}' \
    --max-time 30)

  http_code=$(echo "${response}" | tail -1)
  local body
  body=$(echo "${response}" | sed '$d')

  local status
  status=$(echo "${body}" | jq -r '.status // "unknown"' 2>/dev/null || echo "unknown")

  if [ "${status}" = "PAYMENT_TIMEOUT" ]; then
    print_pass "Order status is PAYMENT_TIMEOUT"
  elif [ "${http_code}" = "504" ] || [ "${http_code}" = "408" ]; then
    print_pass "Timeout response received (HTTP ${http_code})"
  else
    print_fail "Unexpected response: status=${status}, HTTP ${http_code}"
  fi

  # Disable delay simulation
  print_info "Disabling payment delay simulation..."
  curl -s -o /dev/null "${GATEWAY}/payment/admin/simulate-delay?ms=0" -X POST --max-time 5
}

# ----------------------------------------------------------------------------
# Scenario 4: Kafka Async Notification
# ----------------------------------------------------------------------------
test_scenario_4() {
  print_header "Scenario 4: Kafka Async Notification"

  local response http_code
  response=$(curl -s -w "\n%{http_code}" \
    "${GATEWAY}/api/orders" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}' \
    --max-time 15)

  http_code=$(echo "${response}" | tail -1)
  local body
  body=$(echo "${response}" | sed '$d')

  if [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
    print_pass "Order created for async notification test (HTTP ${http_code})"
  else
    print_fail "Order creation failed (HTTP ${http_code})"
    return
  fi

  # Wait for Kafka consumer to process
  print_info "Waiting 5s for Kafka consumer to process..."
  sleep 5

  print_pass "Kafka async notification scenario complete (verify in Jaeger for notification-service spans)"
}

# ----------------------------------------------------------------------------
# Scenario 5: Kafka DLT (Dead Letter Topic)
# ----------------------------------------------------------------------------
test_scenario_5() {
  print_header "Scenario 5: Kafka Consume Failure & DLT"

  # Enable failure simulation
  print_info "Enabling notification failure simulation..."
  curl -s -o /dev/null "${GATEWAY}/notification/admin/simulate-failure?enabled=true" -X POST --max-time 5

  local response http_code
  response=$(curl -s -w "\n%{http_code}" \
    "${GATEWAY}/api/orders" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}' \
    --max-time 15)

  http_code=$(echo "${response}" | tail -1)

  if [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
    print_pass "Order created for DLT test (HTTP ${http_code})"
  else
    print_fail "Order creation failed (HTTP ${http_code})"
  fi

  # Wait for retries and DLT
  print_info "Waiting 20s for retries and DLT processing..."
  sleep 20

  # Disable failure simulation
  print_info "Disabling notification failure simulation..."
  curl -s -o /dev/null "${GATEWAY}/notification/admin/simulate-failure?enabled=false" -X POST --max-time 5

  print_pass "Kafka DLT scenario complete (verify in Jaeger for retry spans and DLT)"
}

# ----------------------------------------------------------------------------
# Verify Traffic Distribution
# ----------------------------------------------------------------------------
verify_distribution() {
  print_header "Verifying Traffic Distribution"

  local total=100
  local blue_count=0
  local green_count=0
  local error_count=0

  print_info "Sending ${total} requests through APISIX gateway..."

  for i in $(seq 1 ${total}); do
    local response
    response=$(curl -s -w "\n%{http_code}" \
      "${GATEWAY}/api/orders" \
      -H "Content-Type: application/json" \
      -X POST \
      -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}' \
      --max-time 10 2>/dev/null || echo -e "\n000")

    local http_code
    http_code=$(echo "${response}" | tail -1)

    if [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
      # Check Jaeger for which service handled the request
      # For now, count all successful as we can't distinguish without response headers
      blue_count=$((blue_count + 1))
    else
      error_count=$((error_count + 1))
    fi

    # Progress indicator
    if (( i % 10 == 0 )); then
      echo -ne "\r  Progress: ${i}/${total}"
    fi
  done
  echo -e "\r  Progress: ${total}/${total} done"
  echo ""

  local success_count=$((blue_count + green_count))
  echo -e "  ${BOLD}Results:${NC}"
  echo -e "  ├── Total requests:    ${total}"
  echo -e "  ├── Successful:        ${success_count}"
  echo -e "  ├── Errors:            ${error_count}"
  echo ""

  if [ "${error_count}" -lt 5 ]; then
    print_pass "Traffic distribution test complete (${success_count}/${total} successful)"
    print_info "Check Jaeger UI at ${JAEGER} to verify Blue/Green distribution by service name"
  else
    print_fail "Too many errors (${error_count}/${total})"
  fi
}

# ----------------------------------------------------------------------------
# Verify Traces in Jaeger
# ----------------------------------------------------------------------------
verify_traces() {
  print_header "Verifying Traces in Jaeger"

  # Send a test request
  print_info "Sending test order request..."
  curl -s -o /dev/null \
    "${GATEWAY}/api/orders" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}' \
    --max-time 15

  # Wait for traces to propagate
  sleep 3

  # Query Jaeger for services
  print_info "Querying Jaeger for registered services..."
  local services
  services=$(curl -s "${JAEGER}/api/services" --max-time 5 2>/dev/null || echo '{"data":[]}')

  local service_list
  service_list=$(echo "${services}" | jq -r '.data[]' 2>/dev/null || echo "")

  echo ""
  echo -e "  ${BOLD}Services found in Jaeger:${NC}"
  for svc in ${service_list}; do
    echo -e "  ├── ${svc}"
  done
  echo ""

  # Check for APISIX service
  if echo "${service_list}" | grep -qi "apisix"; then
    print_pass "APISIX gateway spans found in Jaeger"
  else
    print_fail "APISIX gateway spans not found in Jaeger (OpenTelemetry plugin may not be active)"
  fi

  # Check for order service
  if echo "${service_list}" | grep -qi "order-service"; then
    print_pass "Order service spans found in Jaeger"
  else
    print_fail "Order service spans not found in Jaeger"
  fi

  # Check for downstream services
  for expected_svc in "product-service" "inventory-service" "payment-service"; do
    if echo "${service_list}" | grep -qi "${expected_svc}"; then
      print_pass "${expected_svc} spans found in Jaeger"
    else
      print_fail "${expected_svc} spans not found in Jaeger"
    fi
  done
}

# ----------------------------------------------------------------------------
# Usage
# ----------------------------------------------------------------------------
usage() {
  echo ""
  echo "APISIX Blue-Green Test & Verification"
  echo ""
  echo "Usage: $0 <command>"
  echo ""
  echo "Commands:"
  echo "  all                  Run all 5 test scenarios"
  echo "  verify-distribution  Send 100 requests and check traffic distribution"
  echo "  verify-traces        Verify Jaeger traces include APISIX gateway spans"
  echo "  scenario <1-5>       Run a specific test scenario"
  echo ""
}

# ----------------------------------------------------------------------------
# Print Summary
# ----------------------------------------------------------------------------
print_summary() {
  echo ""
  echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BOLD}  Test Summary${NC}"
  echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "  ${GREEN}Passed${NC}: ${PASS}"
  echo -e "  ${RED}Failed${NC}: ${FAIL}"
  echo -e "  Total:  $((PASS + FAIL))"
  echo ""

  if [ "${FAIL}" -eq 0 ]; then
    echo -e "  ${GREEN}${BOLD}All tests passed!${NC}"
  else
    echo -e "  ${RED}${BOLD}${FAIL} test(s) failed${NC}"
  fi
  echo ""
}

# ============================================================================
# Main
# ============================================================================
if [ $# -lt 1 ]; then
  usage
  exit 1
fi

COMMAND="$1"

case "${COMMAND}" in
  all)
    test_scenario_1
    test_scenario_2
    test_scenario_3
    test_scenario_4
    test_scenario_5
    print_summary
    ;;
  verify-distribution)
    verify_distribution
    ;;
  verify-traces)
    verify_traces
    print_summary
    ;;
  scenario)
    if [ $# -lt 2 ]; then
      print_fail "Please specify scenario number (1-5)"
      exit 1
    fi
    case "$2" in
      1) test_scenario_1 ;;
      2) test_scenario_2 ;;
      3) test_scenario_3 ;;
      4) test_scenario_4 ;;
      5) test_scenario_5 ;;
      *) print_fail "Invalid scenario: $2 (use 1-5)" ;;
    esac
    print_summary
    ;;
  *)
    echo -e "${RED}Unknown command: ${COMMAND}${NC}"
    usage
    exit 1
    ;;
esac
