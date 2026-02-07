#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# APISIX Blue-Green Teardown Script
# Removes the Kind cluster and all associated resources
# ============================================================================

CLUSTER_NAME="apisix-ecommerce"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

print_info() { echo -e "${CYAN}[INFO]${NC} $1"; }
print_ok()   { echo -e "${GREEN}[OK]${NC} $1"; }

echo -e "${BOLD}${RED}"
echo "  ╔═══════════════════════════════════════════════════════╗"
echo "  ║  APISIX Blue-Green Teardown                          ║"
echo "  ╚═══════════════════════════════════════════════════════╝"
echo -e "${NC}"

if ! kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  print_info "Cluster '${CLUSTER_NAME}' does not exist. Nothing to tear down."
  exit 0
fi

print_info "Deleting Kind cluster '${CLUSTER_NAME}'..."
kind delete cluster --name "${CLUSTER_NAME}"

# Verify removal
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  echo -e "${RED}[ERROR]${NC} Cluster '${CLUSTER_NAME}' still exists after deletion attempt"
  exit 1
fi

print_ok "Kind cluster '${CLUSTER_NAME}' has been completely removed"
print_ok "All associated resources (pods, services, namespaces) have been cleaned up"
echo ""
