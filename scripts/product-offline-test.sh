#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Product Service Offline / Recovery Scenario Test
# 模擬 product-service 離線 20 秒後恢復，驗證分散式追蹤與錯誤傳播
# Usage: ./scripts/product-offline-test.sh
# ============================================================================

GATEWAY="${APISIX_GATEWAY_URL:-http://localhost:9080}"
JAEGER="${JAEGER_URL:-http://localhost:16686}"
NAMESPACE="ecommerce"
DEPLOYMENT="product-service"

# Colors (same as apisix-test.sh)
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
print_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }

# Send an order request, return "$http_code|$body"
send_order() {
  local product_id="${1:-P001}"
  local response
  response=$(curl -s -w "\n%{http_code}" \
    "${GATEWAY}/api/orders" \
    -H "Content-Type: application/json" \
    -X POST \
    -d "{\"customerId\":\"C001\",\"items\":[{\"productId\":\"${product_id}\",\"quantity\":1}]}" \
    --max-time 15 2>/dev/null || echo -e "\n000")

  local http_code body
  http_code=$(echo "${response}" | tail -1)
  body=$(echo "${response}" | sed '$d')
  echo "${http_code}|${body}"
}

# ============================================================================
# Phase A: Before — Baseline (正常狀態基準線)
# ============================================================================
phase_before() {
  print_header "Phase A: Before — Baseline (正常狀態)"

  local success=0
  local products=("P001" "P002" "P003")

  for i in 1 2 3; do
    local pid="${products[$((i-1))]}"
    local result http_code body
    result=$(send_order "${pid}")
    http_code="${result%%|*}"
    body="${result#*|}"

    if [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
      print_pass "Baseline request #${i} — HTTP ${http_code} (product: ${pid})"
      success=$((success + 1))
    else
      print_fail "Baseline request #${i} — HTTP ${http_code} (product: ${pid})"
      echo "  Response: ${body}"
    fi
    sleep 1
  done

  if [ "${success}" -eq 3 ]; then
    print_info "All baseline requests succeeded — product-service is healthy"
  else
    print_warn "Some baseline requests failed — check service health before proceeding"
  fi
}

# ============================================================================
# Phase B: Outage — product-service 離線
# ============================================================================
phase_outage() {
  print_header "Phase B: Outage — Scaling Down product-service"

  print_info "Scaling ${DEPLOYMENT} to 0 replicas..."
  kubectl scale deployment "${DEPLOYMENT}" -n "${NAMESPACE}" --replicas=0

  print_info "Waiting 8s for pod termination..."
  sleep 8

  # Verify pod is gone
  local pod_count
  pod_count=$(kubectl get pods -n "${NAMESPACE}" -l app="${DEPLOYMENT}" --no-headers 2>/dev/null | grep -c "Running" || true)
  pod_count=${pod_count:-0}
  if [ "${pod_count}" -eq 0 ]; then
    print_pass "product-service is offline (0 running pods)"
  else
    print_warn "product-service still has ${pod_count} running pods"
  fi

  echo ""
  print_info "Sending requests during outage (expect failures)..."

  local fail_count=0
  for i in 1 2 3; do
    local result http_code body
    result=$(send_order "P001")
    http_code="${result%%|*}"
    body="${result#*|}"

    if [ "${http_code}" = "500" ] || [ "${http_code}" = "502" ] || [ "${http_code}" = "503" ] || [ "${http_code}" = "000" ]; then
      print_pass "Outage request #${i} — HTTP ${http_code} (expected failure)"
      fail_count=$((fail_count + 1))
    elif [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
      print_warn "Outage request #${i} — HTTP ${http_code} (unexpected success — cached or delayed?)"
    else
      print_pass "Outage request #${i} — HTTP ${http_code} (error response)"
      fail_count=$((fail_count + 1))
    fi
    sleep 2
  done

  if [ "${fail_count}" -ge 2 ]; then
    print_info "Outage verified — ${fail_count}/3 requests failed as expected"
  fi

  # Keep offline for remaining time to reach ~20s total outage
  local remaining_wait=6
  print_info "Maintaining outage for ${remaining_wait}s more (total ~20s)..."
  sleep "${remaining_wait}"
}

# ============================================================================
# Phase C: Recovery — product-service 恢復
# ============================================================================
phase_recovery() {
  print_header "Phase C: Recovery — Scaling Up product-service"

  local recovery_start
  recovery_start=$(date +%s)

  print_info "Scaling ${DEPLOYMENT} back to 1 replica..."
  kubectl scale deployment "${DEPLOYMENT}" -n "${NAMESPACE}" --replicas=1

  print_info "Waiting for pod to become ready (timeout 120s)..."
  if kubectl wait --for=condition=ready pod -l "app=${DEPLOYMENT}" -n "${NAMESPACE}" --timeout=120s 2>/dev/null; then
    local recovery_end elapsed
    recovery_end=$(date +%s)
    elapsed=$((recovery_end - recovery_start))
    print_pass "product-service recovered in ${elapsed}s"
  else
    print_fail "product-service did not recover within 120s"
    return 1
  fi

  # Extra wait for APISIX health check to mark upstream healthy
  print_info "Waiting 10s for APISIX upstream health check..."
  sleep 10
}

# ============================================================================
# Phase D: After — 驗證恢復
# ============================================================================
phase_after() {
  print_header "Phase D: After — Verify Recovery (恢復驗證)"

  local success=0
  local products=("P001" "P002" "P003")

  for i in 1 2 3; do
    local pid="${products[$((i-1))]}"
    local result http_code body
    result=$(send_order "${pid}")
    http_code="${result%%|*}"
    body="${result#*|}"

    if [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
      print_pass "Recovery request #${i} — HTTP ${http_code} (product: ${pid})"
      success=$((success + 1))
    else
      print_fail "Recovery request #${i} — HTTP ${http_code} (product: ${pid})"
      echo "  Response: ${body}"
    fi
    sleep 1
  done

  if [ "${success}" -eq 3 ]; then
    print_info "All recovery requests succeeded — product-service fully recovered"
  else
    print_warn "Some recovery requests failed — APISIX may still be marking upstream as unhealthy"
  fi
}

# ============================================================================
# Print Summary
# ============================================================================
print_summary() {
  echo ""
  echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BOLD}  Product Service Offline Scenario — Summary${NC}"
  echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "  ${GREEN}Passed${NC}: ${PASS}"
  echo -e "  ${RED}Failed${NC}: ${FAIL}"
  echo -e "  Total:  $((PASS + FAIL))"
  echo ""

  if [ "${FAIL}" -eq 0 ]; then
    echo -e "  ${GREEN}${BOLD}All checks passed!${NC}"
  else
    echo -e "  ${RED}${BOLD}${FAIL} check(s) failed${NC}"
  fi
  echo ""
  echo -e "${CYAN}[INFO]${NC} Check Jaeger UI:  ${JAEGER}"
  echo -e "${CYAN}[INFO]${NC} Check Grafana:    http://localhost:30300"
  echo ""
}

# ============================================================================
# Main
# ============================================================================
echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║  Product Service Offline / Recovery Scenario Test   ║${NC}"
echo -e "${BOLD}╚══════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}[INFO]${NC} Gateway:   ${GATEWAY}"
echo -e "${CYAN}[INFO]${NC} Namespace: ${NAMESPACE}"
echo -e "${CYAN}[INFO]${NC} Target:    ${DEPLOYMENT}"
echo -e "${CYAN}[INFO]${NC} Started:   $(date '+%Y-%m-%d %H:%M:%S')"

SCENARIO_START=$(date +%s)

phase_before
phase_outage
phase_recovery
phase_after

SCENARIO_END=$(date +%s)
TOTAL_DURATION=$((SCENARIO_END - SCENARIO_START))

echo ""
print_info "Total scenario duration: ${TOTAL_DURATION}s"
print_summary
