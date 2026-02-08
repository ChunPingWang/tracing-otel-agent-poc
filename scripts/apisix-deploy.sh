#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# APISIX Blue-Green Deployment Script
# One-command deployment: Kind cluster + APISIX + Blue/Green + all services
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
K8S_DIR="${PROJECT_ROOT}/apisix-k8s"
CLUSTER_NAME="apisix-ecommerce"

APISIX_ADMIN="http://localhost:9180/apisix/admin"
APISIX_API_KEY="poc-admin-key-2024"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

print_header() { echo -e "\n${BOLD}${BLUE}=== $1 ===${NC}\n"; }
print_info()   { echo -e "${CYAN}[INFO]${NC} $1"; }
print_ok()     { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn()   { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error()  { echo -e "${RED}[ERROR]${NC} $1"; }

# ============================================================================
# Step 0: Prerequisite Checks
# ============================================================================
check_prerequisites() {
  print_header "Step 0: Checking Prerequisites"

  local missing=0
  for cmd in docker kind kubectl helm jq curl; do
    if command -v "${cmd}" &>/dev/null; then
      print_ok "${cmd} found: $(${cmd} version --short 2>/dev/null || ${cmd} --version 2>/dev/null | head -1 || echo 'installed')"
    else
      print_error "${cmd} is not installed"
      missing=1
    fi
  done

  if [ "${missing}" -eq 1 ]; then
    print_error "Missing prerequisites. Please install the required tools and try again."
    exit 1
  fi

  # Check Docker daemon
  if ! docker info &>/dev/null; then
    print_error "Docker daemon is not running. Please start Docker and try again."
    exit 1
  fi

  print_ok "All prerequisites met"
}

# ============================================================================
# Step 1: Create Kind Cluster
# ============================================================================
create_cluster() {
  print_header "Step 1: Creating Kind Cluster"

  if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
    print_warn "Cluster '${CLUSTER_NAME}' already exists"
    read -rp "Delete existing cluster and recreate? (y/N): " answer
    if [[ "${answer}" =~ ^[Yy]$ ]]; then
      kind delete cluster --name "${CLUSTER_NAME}"
      print_ok "Existing cluster deleted"
    else
      print_info "Using existing cluster"
      return 0
    fi
  fi

  kind create cluster --name "${CLUSTER_NAME}" --config "${K8S_DIR}/kind-config.yaml"
  print_ok "Kind cluster '${CLUSTER_NAME}' created"

  # Verify cluster
  kubectl cluster-info --context "kind-${CLUSTER_NAME}"
  print_ok "Cluster is ready"
}

# ============================================================================
# Step 2: Create Namespace
# ============================================================================
create_namespace() {
  print_header "Step 2: Creating Namespace"

  kubectl apply -f "${K8S_DIR}/namespace.yaml"
  print_ok "Namespace 'ecommerce' created"
}

# ============================================================================
# Step 3: Build and Load Docker Images
# ============================================================================
build_and_load_images() {
  print_header "Step 3: Building and Loading Docker Images"

  local services=("order-service" "product-service" "inventory-service" "payment-service" "notification-service")

  for svc in "${services[@]}"; do
    print_info "Building ${svc}..."
    docker build -t "${svc}:latest" "${PROJECT_ROOT}/${svc}" -q
    print_ok "Built ${svc}:latest"
  done

  for svc in "${services[@]}"; do
    print_info "Loading ${svc} into Kind..."
    kind load docker-image "${svc}:latest" --name "${CLUSTER_NAME}"
    print_ok "Loaded ${svc}:latest"
  done

  print_ok "All images built and loaded"
}

# ============================================================================
# Step 4: Deploy Kafka & Jaeger
# ============================================================================
deploy_base_services() {
  print_header "Step 4: Deploying Kafka & Jaeger"

  kubectl apply -f "${K8S_DIR}/kafka/" -n ecommerce
  kubectl apply -f "${K8S_DIR}/jaeger/" -n ecommerce

  print_info "Waiting for Kafka to be ready..."
  kubectl wait --for=condition=ready pod -l app=kafka -n ecommerce --timeout=300s
  print_ok "Kafka is ready"

  print_info "Waiting for Jaeger to be ready..."
  kubectl wait --for=condition=ready pod -l app=jaeger -n ecommerce --timeout=120s
  print_ok "Jaeger is ready"
}

# ============================================================================
# Step 5: Deploy Prometheus & Grafana
# ============================================================================
deploy_monitoring() {
  print_header "Step 5: Deploying Prometheus & Grafana"

  kubectl apply -f "${K8S_DIR}/prometheus/" -n ecommerce
  print_info "Applied Prometheus manifests"

  kubectl apply -f "${K8S_DIR}/grafana/" -n ecommerce
  print_info "Applied Grafana manifests"

  print_info "Waiting for Prometheus to be ready..."
  kubectl wait --for=condition=ready pod -l app=prometheus -n ecommerce --timeout=120s
  print_ok "Prometheus is ready"

  print_info "Waiting for Grafana to be ready..."
  kubectl wait --for=condition=ready pod -l app=grafana -n ecommerce --timeout=120s
  print_ok "Grafana is ready"
}

# ============================================================================
# Step 6: Deploy Microservices
# ============================================================================
deploy_microservices() {
  print_header "Step 6: Deploying Microservices"

  local services=("product-service" "inventory-service" "payment-service" "notification-service" "order-service-blue" "order-service-green")

  for svc in "${services[@]}"; do
    kubectl apply -f "${K8S_DIR}/${svc}/" -n ecommerce
    print_info "Applied ${svc} manifests"
  done

  print_info "Waiting for all microservices to be ready (this may take 2-3 minutes)..."
  for svc in "${services[@]}"; do
    local label_app
    if [[ "${svc}" == "order-service-blue" ]]; then
      label_app="app=order-service,version=blue"
    elif [[ "${svc}" == "order-service-green" ]]; then
      label_app="app=order-service,version=green"
    else
      label_app="app=${svc}"
    fi
    kubectl wait --for=condition=ready pod -l "${label_app}" -n ecommerce --timeout=180s
    print_ok "${svc} is ready"
  done

  print_ok "All microservices are ready"
}

# ============================================================================
# Step 7: Install APISIX via Helm
# ============================================================================
install_apisix() {
  print_header "Step 7: Installing APISIX via Helm"

  helm repo add apisix https://apache.github.io/apisix-helm-chart 2>/dev/null || true
  helm repo update

  if helm status apisix -n apisix &>/dev/null; then
    print_warn "APISIX Helm release already exists, upgrading..."
    helm upgrade apisix apisix/apisix \
      --namespace apisix \
      -f "${K8S_DIR}/apisix-values.yaml" \
      --wait --timeout 300s
  else
    helm install apisix apisix/apisix \
      --create-namespace --namespace apisix \
      -f "${K8S_DIR}/apisix-values.yaml" \
      --wait --timeout 300s
  fi

  print_info "Waiting for APISIX gateway to be ready..."
  kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=apisix -n apisix --timeout=180s
  print_ok "APISIX gateway is ready"
}

# ============================================================================
# Step 8: Configure APISIX Routes & Upstreams
# ============================================================================
configure_apisix() {
  print_header "Step 8: Configuring APISIX Routes & Upstreams"

  # Wait for Admin API to be reachable
  print_info "Waiting for Admin API to be reachable..."
  local max_retries=30
  local retry=0
  while ! curl -s -o /dev/null -w "%{http_code}" \
    "${APISIX_ADMIN}/routes" \
    -H "X-API-KEY: ${APISIX_API_KEY}" 2>/dev/null | grep -q "200"; do
    retry=$((retry + 1))
    if [ "${retry}" -ge "${max_retries}" ]; then
      print_error "Admin API not reachable after ${max_retries} retries"
      exit 1
    fi
    sleep 2
  done
  print_ok "Admin API is reachable"

  # Create upstreams
  print_info "Creating upstreams..."
  local upstreams
  upstreams=$(cat "${K8S_DIR}/apisix-config/upstreams.json")
  local upstream_count
  upstream_count=$(echo "${upstreams}" | jq length)

  for i in $(seq 0 $((upstream_count - 1))); do
    local upstream
    upstream=$(echo "${upstreams}" | jq ".[$i]")
    local upstream_id
    upstream_id=$(echo "${upstream}" | jq -r '.id')
    local upstream_name
    upstream_name=$(echo "${upstream}" | jq -r '.name')

    curl -s -o /dev/null -w "" \
      "${APISIX_ADMIN}/upstreams/${upstream_id}" \
      -H "X-API-KEY: ${APISIX_API_KEY}" \
      -H "Content-Type: application/json" \
      -X PUT \
      -d "${upstream}"
    print_ok "Created upstream ${upstream_id}: ${upstream_name}"
  done

  # Create routes
  print_info "Creating routes..."
  local routes
  routes=$(cat "${K8S_DIR}/apisix-config/route.json")
  local route_count
  route_count=$(echo "${routes}" | jq length)

  for i in $(seq 0 $((route_count - 1))); do
    local route
    route=$(echo "${routes}" | jq ".[$i]")
    local route_id
    route_id=$(echo "${route}" | jq -r '.id')
    local route_name
    route_name=$(echo "${route}" | jq -r '.name')

    curl -s -o /dev/null -w "" \
      "${APISIX_ADMIN}/routes/${route_id}" \
      -H "X-API-KEY: ${APISIX_API_KEY}" \
      -H "Content-Type: application/json" \
      -X PUT \
      -d "${route}"
    print_ok "Created route ${route_id}: ${route_name}"
  done

  # Create global rules (OpenTelemetry)
  print_info "Creating global rules..."
  local global_rules
  global_rules=$(cat "${K8S_DIR}/apisix-config/global-rules.json")
  local rule_count
  rule_count=$(echo "${global_rules}" | jq length)

  for i in $(seq 0 $((rule_count - 1))); do
    local rule
    rule=$(echo "${global_rules}" | jq ".[$i]")
    local rule_id
    rule_id=$(echo "${rule}" | jq -r '.id')

    curl -s -o /dev/null -w "" \
      "${APISIX_ADMIN}/global_rules/${rule_id}" \
      -H "X-API-KEY: ${APISIX_API_KEY}" \
      -H "Content-Type: application/json" \
      -X PUT \
      -d "${rule}"
    print_ok "Created global rule ${rule_id}: OpenTelemetry"
  done

  print_ok "APISIX configuration complete"
}

# ============================================================================
# Step 9: Verify Deployment
# ============================================================================
verify_deployment() {
  print_header "Step 9: Verifying Deployment"

  # Check all pods
  print_info "Checking pod status..."
  echo ""
  kubectl get pods -n ecommerce -o wide
  echo ""
  kubectl get pods -n apisix -o wide
  echo ""

  # Test gateway
  print_info "Testing APISIX gateway..."
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://localhost:9080/api/orders" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}' \
    --max-time 10 2>/dev/null || echo "000")

  if [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
    print_ok "Gateway is routing requests successfully (HTTP ${http_code})"
  else
    print_warn "Gateway returned HTTP ${http_code} — services may still be starting"
  fi

  # Test Jaeger UI
  local jaeger_code
  jaeger_code=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://localhost:16686/" --max-time 5 2>/dev/null || echo "000")

  if [ "${jaeger_code}" = "200" ]; then
    print_ok "Jaeger UI is accessible"
  else
    print_warn "Jaeger UI returned HTTP ${jaeger_code}"
  fi
}

# ============================================================================
# Print Summary
# ============================================================================
print_summary() {
  print_header "Deployment Complete"

  echo -e "  ${BOLD}Endpoints:${NC}"
  echo -e "  ├── APISIX Gateway:    ${GREEN}http://localhost:9080${NC}"
  echo -e "  ├── APISIX Admin API:  ${CYAN}http://localhost:9180${NC}"
  echo -e "  ├── Jaeger UI:         ${YELLOW}http://localhost:16686${NC}"
  echo -e "  ├── Grafana:           ${GREEN}http://localhost:30300${NC}"
  echo -e "  ├── Prometheus:        ${CYAN}http://localhost:9090${NC} (via port-forward)"
  echo -e "  ├── Order API:         ${GREEN}http://localhost:9080/api/orders${NC}"
  echo -e "  ├── Payment Admin:     ${GREEN}http://localhost:9080/payment/admin/simulate-delay?ms=5000${NC}"
  echo -e "  └── Notification Admin:${GREEN}http://localhost:9080/notification/admin/simulate-failure?enabled=true${NC}"
  echo ""
  echo -e "  ${BOLD}Traffic Control:${NC}"
  echo -e "  ├── All Blue:    ${CYAN}./scripts/apisix-traffic.sh blue${NC}"
  echo -e "  ├── Canary:      ${CYAN}./scripts/apisix-traffic.sh canary${NC}"
  echo -e "  ├── 50/50:       ${CYAN}./scripts/apisix-traffic.sh split${NC}"
  echo -e "  ├── All Green:   ${CYAN}./scripts/apisix-traffic.sh green${NC}"
  echo -e "  ├── Rollback:    ${CYAN}./scripts/apisix-traffic.sh rollback${NC}"
  echo -e "  ├── Header:      ${CYAN}./scripts/apisix-traffic.sh header${NC}"
  echo -e "  └── Status:      ${CYAN}./scripts/apisix-traffic.sh status${NC}"
  echo ""
  echo -e "  ${BOLD}Teardown:${NC}"
  echo -e "  └── ${RED}./scripts/apisix-teardown.sh${NC}"
  echo ""
}

# ============================================================================
# Main
# ============================================================================
main() {
  echo -e "${BOLD}${BLUE}"
  echo "  ╔═══════════════════════════════════════════════════════╗"
  echo "  ║  APISIX Blue-Green Deployment for E-Commerce PoC     ║"
  echo "  ╚═══════════════════════════════════════════════════════╝"
  echo -e "${NC}"

  check_prerequisites
  create_cluster
  create_namespace
  build_and_load_images
  deploy_base_services
  deploy_monitoring
  deploy_microservices
  install_apisix
  configure_apisix
  verify_deployment
  print_summary
}

main "$@"
