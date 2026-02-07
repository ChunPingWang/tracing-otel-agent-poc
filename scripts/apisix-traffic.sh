#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# APISIX Blue-Green Traffic Control Script
# Usage: ./scripts/apisix-traffic.sh <command>
# Commands: blue | canary | split | green | rollback | header | status
# ============================================================================

APISIX_ADMIN="${APISIX_ADMIN_URL:-http://localhost:9180/apisix/admin}"
APISIX_API_KEY="${APISIX_API_KEY:-poc-admin-key-2024}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

print_info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
print_ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ----------------------------------------------------------------------------
# Check Admin API reachability
# ----------------------------------------------------------------------------
check_admin_api() {
  if ! curl -s -o /dev/null -w "%{http_code}" \
    "${APISIX_ADMIN}/routes" \
    -H "X-API-KEY: ${APISIX_API_KEY}" | grep -q "200"; then
    print_error "APISIX Admin API is not reachable at ${APISIX_ADMIN}"
    print_error "Ensure the APISIX gateway is running and the admin API is accessible."
    exit 1
  fi
}

# ----------------------------------------------------------------------------
# Update traffic-split configuration
# Args: $1=green_weight, $2=blue_weight, $3=description
# ----------------------------------------------------------------------------
update_traffic_split() {
  local green_weight="$1"
  local blue_weight="$2"
  local description="$3"

  local payload
  payload=$(cat <<EOF
{
  "plugins": {
    "traffic-split": {
      "rules": [
        {
          "match": [
            {
              "vars": [["http_X-Canary", "==", "true"]]
            }
          ],
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": 1 }
          ]
        },
        {
          "weighted_upstreams": [
            { "upstream_id": "2", "weight": ${green_weight} },
            { "weight": ${blue_weight} }
          ]
        }
      ]
    }
  }
}
EOF
)

  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    "${APISIX_ADMIN}/routes/1" \
    -H "X-API-KEY: ${APISIX_API_KEY}" \
    -H "Content-Type: application/json" \
    -X PATCH \
    -d "${payload}")

  if [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
    print_ok "${description}"
    echo ""
    echo -e "  ${BLUE}Blue (v1)${NC}:  ${blue_weight}%"
    echo -e "  ${GREEN}Green (v2)${NC}: ${green_weight}%"
    echo -e "  ${YELLOW}X-Canary${NC}:   always → Green"
    echo ""
  else
    print_error "Failed to update traffic split (HTTP ${http_code})"
    exit 1
  fi
}

# ----------------------------------------------------------------------------
# Show current traffic configuration
# ----------------------------------------------------------------------------
show_status() {
  print_info "Fetching current route configuration..."
  echo ""

  local response
  response=$(curl -s "${APISIX_ADMIN}/routes/1" \
    -H "X-API-KEY: ${APISIX_API_KEY}")

  if [ -z "${response}" ]; then
    print_error "Failed to fetch route configuration"
    exit 1
  fi

  # Parse weights from the traffic-split rules (second rule = weighted split)
  local green_weight blue_weight
  green_weight=$(echo "${response}" | jq -r '.value.plugins."traffic-split".rules[1].weighted_upstreams[0].weight // "N/A"')
  blue_weight=$(echo "${response}" | jq -r '.value.plugins."traffic-split".rules[1].weighted_upstreams[1].weight // "N/A"')

  # Check if header-based routing is active
  local header_match
  header_match=$(echo "${response}" | jq -r '.value.plugins."traffic-split".rules[0].match[0].vars[0][0] // "none"')

  echo "  ┌─────────────────────────────────────┐"
  echo "  │   APISIX Traffic Configuration       │"
  echo "  ├─────────────────────────────────────┤"
  echo -e "  │  ${BLUE}Blue (v1)${NC}:  ${blue_weight}%                   │"
  echo -e "  │  ${GREEN}Green (v2)${NC}: ${green_weight}%                   │"
  if [ "${header_match}" = "http_X-Canary" ]; then
    echo -e "  │  ${YELLOW}X-Canary${NC}:   always → Green        │"
  fi
  echo "  └─────────────────────────────────────┘"
  echo ""
}

# ----------------------------------------------------------------------------
# Usage
# ----------------------------------------------------------------------------
usage() {
  echo ""
  echo "APISIX Blue-Green Traffic Control"
  echo ""
  echo "Usage: $0 <command>"
  echo ""
  echo "Commands:"
  echo "  blue      Set 100% Blue / 0% Green (initial state)"
  echo "  canary    Set 90% Blue / 10% Green (canary release)"
  echo "  split     Set 50% Blue / 50% Green (equal split)"
  echo "  green     Set 0% Blue / 100% Green (full cutover)"
  echo "  rollback  Immediately revert to 100% Blue (same as 'blue')"
  echo "  header    Set 100% Blue + X-Canary:true → Green (QA testing)"
  echo "  status    Show current traffic configuration"
  echo ""
  echo "Environment Variables:"
  echo "  APISIX_ADMIN_URL  Admin API URL (default: http://localhost:9180/apisix/admin)"
  echo "  APISIX_API_KEY    Admin API key (default: poc-admin-key-2024)"
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
  blue)
    check_admin_api
    update_traffic_split 0 100 "Traffic set to 100% Blue (v1)"
    ;;
  canary)
    check_admin_api
    update_traffic_split 10 90 "Traffic set to 90% Blue / 10% Green (canary)"
    ;;
  split)
    check_admin_api
    update_traffic_split 50 50 "Traffic set to 50% Blue / 50% Green"
    ;;
  green)
    check_admin_api
    update_traffic_split 100 0 "Traffic set to 100% Green (v2) — full cutover"
    ;;
  rollback)
    check_admin_api
    echo -e "${RED}>>> ROLLBACK initiated <<<${NC}"
    update_traffic_split 0 100 "ROLLBACK complete — all traffic reverted to Blue (v1)"
    # Verify rollback
    print_info "Verifying rollback..."
    local_green_weight=$(curl -s "${APISIX_ADMIN}/routes/1" \
      -H "X-API-KEY: ${APISIX_API_KEY}" | \
      jq -r '.value.plugins."traffic-split".rules[1].weighted_upstreams[0].weight')
    if [ "${local_green_weight}" = "0" ]; then
      print_ok "Rollback verified — Green weight is 0"
    else
      print_warn "Rollback verification: Green weight is ${local_green_weight} (expected 0)"
    fi
    ;;
  header)
    check_admin_api
    update_traffic_split 0 100 "Header-based routing active: X-Canary:true → Green, all other → Blue"
    print_info "Test with: curl -H 'X-Canary: true' http://localhost:9080/api/orders"
    ;;
  status)
    check_admin_api
    show_status
    ;;
  *)
    print_error "Unknown command: ${COMMAND}"
    usage
    exit 1
    ;;
esac
