# APISIX Blue-Green Deployment for E-Commerce PoC

Apache APISIX 作為 API Gateway，在 Kind Kubernetes 叢集上實現 order-service 的藍綠部署與加權流量切換。

## 架構總覽

```
                        ┌──────────────────────────────────┐
                        │         Kind Cluster             │
                        │       (apisix-ecommerce)         │
                        │                                  │
 Client ──► :9080 ──►  │  ┌──────────────────────┐        │
                        │  │   Apache APISIX       │        │
                        │  │   (apisix namespace)  │        │
                        │  │                      │        │
                        │  │  traffic-split plugin │        │
                        │  │  opentelemetry plugin │        │
                        │  └──────┬───────┬───────┘        │
                        │         │       │                │
                        │    ┌────▼──┐ ┌──▼────┐           │
                        │    │ Blue  │ │ Green │           │
                        │    │ v1    │ │ v2    │           │
                        │    │:8081  │ │:8081  │           │
                        │    └───┬───┘ └───┬───┘           │
                        │        │         │               │
                        │   ┌────▼─────────▼────┐          │
                        │   │  Downstream Svcs   │          │
                        │   │  product  :8082    │          │
                        │   │  inventory :8083   │          │
                        │   │  payment  :8084    │          │
                        │   │  notification:8085 │          │
                        │   └────────┬───────────┘          │
                        │            │                     │
                        │   ┌────────▼───────────┐          │
                        │   │  Kafka :9092       │          │
                        │   │  (KRaft mode)      │          │
                        │   └────────────────────┘          │
                        │                                  │
                        │   ┌────────────────────┐          │
 Jaeger UI ◄── :16686 ◄│   │  Jaeger :4317/4318 │          │
                        │   │  (all-in-one)      │          │
                        │   └────────────────────┘          │
                        │                                  │
                        │   ┌────────────────────┐          │
                        │   │  Prometheus :9090   │          │
                        │   │  (OTLP Receiver)   │◄─ metrics│
                        │   └────────┬───────────┘          │
                        │            │                     │
                        │   ┌────────▼───────────┐          │
 Grafana ◄── :30300  ◄─│   │  Grafana :3000     │          │
                        │   │  (3 Dashboards +   │          │
                        │   │   4 Alert Rules +  │          │
                        │   │   Webhook Alerts)  │          │
                        │   └────────┬───────────┘          │
                        │            │ alerts               │
                        │   ┌────────▼───────────┐          │
                        │   │  Webhook Sink :8080 │          │
                        │   │  (Alert Receiver)  │          │
                        │   └────────────────────┘          │
                        └──────────────────────────────────┘
```

**流量切換方式**：APISIX `traffic-split` 外掛根據權重將請求分配至 Blue/Green upstream，同時支援 `X-Canary: true` header 直接導向 Green。

**分散式追蹤**：APISIX `opentelemetry` 外掛保留 W3C Trace Context，Jaeger 可看到完整 span chain（含 Gateway span）。

## 前置需求

| 工具 | 最低版本 | 檢查指令 |
|------|---------|---------|
| Docker | 20.10+ | `docker version` |
| Kind | v0.20+ | `kind version` |
| kubectl | v1.28+ | `kubectl version --client` |
| Helm | 3.14+ | `helm version` |
| jq | 1.6+ | `jq --version` |
| curl | 7.x+ | `curl --version` |

## 快速開始

### 1. 一鍵部署

```bash
./scripts/apisix-deploy.sh
```

部署內容：
- Kind 叢集 `apisix-ecommerce`（port mapping: 9080, 9180, 16686, 30300）
- APISIX Gateway + etcd（`apisix` namespace）
- 5 個微服務 + Kafka + Jaeger + Prometheus + Grafana（`ecommerce` namespace）
- APISIX 路由、upstream、OpenTelemetry 全域規則
- Grafana 預設 3 個 Dashboard：Service Health、JVM Metrics、Kafka Metrics
- Grafana 4 條 Alert Rules：High Error Rate、High Latency、JVM Heap、Kafka Lag
- Grafana Webhook Contact Point + 嚴重等級路由 Notification Policy
- Webhook Sink 告警接收器（記錄告警 payload 至 stdout）

預計耗時約 5-6 分鐘。

### 2. 流量操作

```bash
# 查看目前流量配置
./scripts/apisix-traffic.sh status

# 100% Blue（初始狀態）
./scripts/apisix-traffic.sh blue

# 金絲雀發布：90% Blue / 10% Green
./scripts/apisix-traffic.sh canary

# 均分：50% Blue / 50% Green
./scripts/apisix-traffic.sh split

# 全量切換至 Green
./scripts/apisix-traffic.sh green

# 緊急回滾至 Blue
./scripts/apisix-traffic.sh rollback

# Header 路由（QA 測試用）
./scripts/apisix-traffic.sh header
curl -H "X-Canary: true" http://localhost:9080/api/orders -X POST \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P001","quantity":1}]}'
```

### 3. 執行測試

```bash
# 執行全部 5 個測試情境
./scripts/apisix-test.sh all

# 驗證流量分配（發送 100 個請求）
./scripts/apisix-test.sh verify-distribution

# 驗證 Jaeger 追蹤（含 APISIX Gateway span）
./scripts/apisix-test.sh verify-traces

# 執行單一情境（1-5）
./scripts/apisix-test.sh scenario 1
```

### 4. 清除環境

```bash
./scripts/apisix-teardown.sh
```

## 流量情境參考表

| 指令 | Blue (v1) | Green (v2) | X-Canary Header | 用途 |
|------|-----------|------------|-----------------|------|
| `blue` | 100% | 0% | → Green | 初始狀態 / 回滾 |
| `canary` | 90% | 10% | → Green | 金絲雀發布 |
| `split` | 50% | 50% | → Green | 均分測試 |
| `green` | 0% | 100% | → Green | 全量切換 |
| `rollback` | 100% | 0% | → Green | 緊急回滾 |
| `header` | 100% | 0% | → Green | QA header 路由 |

## Endpoint 參考

| 名稱 | URL | 說明 |
|------|-----|------|
| APISIX Gateway | `http://localhost:9080` | 所有 API 請求入口 |
| APISIX Admin API | `http://localhost:9180` | 路由/upstream 設定 |
| Jaeger UI | `http://localhost:16686` | 追蹤視覺化 |
| Grafana | `http://localhost:30300` | 監控儀表板（匿名存取，無需登入） |
| Order API | `http://localhost:9080/api/orders` | 訂單 API（路由至 Blue/Green）|
| Payment Admin | `http://localhost:9080/payment/admin/*` | 延遲模擬 |
| Notification Admin | `http://localhost:9080/notification/admin/*` | 故障模擬 |

## APISIX Admin API 範例

```bash
# 查看所有路由
curl http://localhost:9180/apisix/admin/routes \
  -H "X-API-KEY: poc-admin-key-2024"

# 查看所有 upstream
curl http://localhost:9180/apisix/admin/upstreams \
  -H "X-API-KEY: poc-admin-key-2024"

# 查看 Route 1 設定（含 traffic-split 權重）
curl http://localhost:9180/apisix/admin/routes/1 \
  -H "X-API-KEY: poc-admin-key-2024" | jq '.value.plugins."traffic-split"'

# 手動調整權重（Green 20% / Blue 80%）
curl http://localhost:9180/apisix/admin/routes/1 \
  -H "X-API-KEY: poc-admin-key-2024" \
  -H "Content-Type: application/json" \
  -X PATCH \
  -d '{
    "plugins": {
      "traffic-split": {
        "rules": [
          {
            "match": [{"vars": [["http_X-Canary", "==", "true"]]}],
            "weighted_upstreams": [{"upstream_id": "2", "weight": 1}]
          },
          {
            "weighted_upstreams": [
              {"upstream_id": "2", "weight": 20},
              {"weight": 80}
            ]
          }
        ]
      }
    }
  }'
```

## 疑難排解

### Pod 未就緒

```bash
# 檢查 pod 狀態
kubectl get pods -n ecommerce
kubectl get pods -n apisix

# 查看特定 pod 日誌
kubectl logs -n ecommerce -l app=order-service,version=blue
kubectl logs -n apisix -l app.kubernetes.io/name=apisix
```

### Admin API 無法連線

```bash
# 確認 APISIX pod 狀態
kubectl get pods -n apisix

# 確認 NodePort 服務
kubectl get svc -n apisix

# 確認 port mapping
docker port apisix-ecommerce-control-plane
```

### Port 衝突

如果 9080、9180 或 16686 被佔用：

```bash
# 檢查佔用的 port
lsof -i :9080
lsof -i :9180
lsof -i :16686

# 先停止佔用的程序，再重新部署
```

### 流量切換無效果

```bash
# 確認 route 設定
./scripts/apisix-traffic.sh status

# 在 Jaeger UI 中搜尋 order-service-blue 和 order-service-green
# 確認請求是否被正確路由
open http://localhost:16686
```

## 目錄結構

```
apisix-k8s/
├── kind-config.yaml              # Kind 叢集配置（port mapping）
├── namespace.yaml                # ecommerce namespace
├── apisix-values.yaml            # APISIX Helm values
├── apisix-config/
│   ├── upstreams.json            # APISIX upstream 定義
│   ├── route.json                # APISIX 路由定義
│   └── global-rules.json         # OpenTelemetry 全域規則
├── kafka/
│   ├── deployment.yaml
│   └── service.yaml
├── jaeger/
│   ├── deployment.yaml
│   └── service.yaml
├── prometheus/
│   ├── configmap.yaml               # Prometheus OTLP receiver 設定
│   ├── deployment.yaml
│   └── service.yaml
├── grafana/
│   ├── configmap-datasources.yaml   # Prometheus 資料來源
│   ├── configmap-dashboard-providers.yaml
│   ├── configmap-dashboards.yaml    # 3 個 Dashboard JSON
│   ├── configmap-alert-rules.yaml   # 4 條 Alert Rules（Error Rate、Latency、JVM Heap、Kafka Lag）
│   ├── configmap-contact-points.yaml # Webhook Contact Point
│   ├── configmap-notification-policies.yaml # 嚴重等級路由 Notification Policy
│   ├── deployment.yaml
│   └── service.yaml                 # NodePort :30300
├── product-service/
│   ├── deployment.yaml
│   └── service.yaml
├── inventory-service/
│   ├── deployment.yaml
│   └── service.yaml
├── payment-service/
│   ├── deployment.yaml
│   └── service.yaml
├── notification-service/
│   ├── deployment.yaml
│   └── service.yaml
├── webhook-sink/
│   ├── deployment.yaml              # mendhak/http-https-echo:39
│   └── service.yaml                 # ClusterIP :8080
├── order-service-blue/
│   ├── deployment.yaml
│   └── service.yaml
└── order-service-green/
    ├── deployment.yaml
    └── service.yaml
```
