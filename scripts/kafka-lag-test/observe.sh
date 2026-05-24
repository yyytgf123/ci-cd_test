#!/usr/bin/env bash
# ============================================================
# observe.sh — KEDA lag 임계값 측정용 관찰 스크립트
# 사용: ./observe.sh <namespace>
# 예시: watch -n 30 ./observe.sh dev
# ============================================================

NS=${1:-dev}
KAFKA="courm-kafka.courm-kafka.svc.cluster.local:9092"
CONSUMER_GROUP="order-service-group"
TOPICS="payment-events product-events"
PROM_URL="http://localhost:9090"

TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
echo "============================================================"
echo " Kafka Lag Observation — $TIMESTAMP"
echo "============================================================"

# ── 1. Kafka Consumer Group Lag ──────────────────────────────
echo ""
echo "[1] Consumer Group Lag (kubectl exec into kafka pod)"
KAFKA_POD=$(kubectl get pod -n courm-kafka -l app=kafka --no-headers 2>/dev/null | head -1 | awk '{print $1}')
if [ -n "$KAFKA_POD" ]; then
  kubectl exec -n courm-kafka "$KAFKA_POD" -- \
    kafka-consumer-groups.sh \
      --bootstrap-server "$KAFKA" \
      --describe \
      --group "$CONSUMER_GROUP" \
    2>/dev/null | grep -E "TOPIC|payment-events|product-events"
else
  echo "  [WARN] Kafka pod not found in courm-kafka namespace"
fi

# ── 2. Prometheus — Lag 합계 쿼리 ────────────────────────────
echo ""
echo "[2] Prometheus kafka_consumergroup_lag_sum"
for TOPIC in $TOPICS; do
  QUERY="sum(kafka_consumergroup_lag{topic=\"${TOPIC}\",consumergroup=\"${CONSUMER_GROUP}\"})"
  ENCODED=$(python3 -c "import urllib.parse; print(urllib.parse.quote('${QUERY}'))")
  RESULT=$(curl -s "${PROM_URL}/api/v1/query?query=${ENCODED}" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); r=d.get('data',{}).get('result',[]); print(r[0]['value'][1] if r else 'N/A')" 2>/dev/null)
  echo "  $TOPIC lag = $RESULT"
done

# ── 3. Pod 수 및 CPU/Memory ──────────────────────────────────
echo ""
echo "[3] Order-Service Pods (namespace=$NS)"
kubectl top pod -n "$NS" -l "app.kubernetes.io/name=courm-app" --no-headers 2>/dev/null \
  | grep order \
  || kubectl get pod -n "$NS" -l "app.kubernetes.io/name=courm-app" --no-headers 2>/dev/null | grep order

# ── 4. KEDA ScaledObject 상태 ─────────────────────────────────
echo ""
echo "[4] KEDA ScaledObject"
kubectl get scaledobject -n "$NS" 2>/dev/null | grep -E "NAME|order" \
  || echo "  (KEDA not enabled yet)"

# ── 5. 처리량 계산 안내 ──────────────────────────────────────
echo ""
echo "------------------------------------------------------------"
echo " lagThreshold 계산 공식:"
echo "   lagThreshold = TPS × 허용_지연_초"
echo "   예) TPS=30, 허용지연=10s → lagThreshold=300"
echo ""
echo " 현재 측정값으로 추정:"
echo "   1. 위 Prometheus lag 값이 안정적으로 수렴하는 지점 확인"
echo "   2. 그 lag 값이 pod 1개로 처리 가능한 최대 TPS × 허용지연 이하인지 확인"
echo "   3. 해당 값을 value-dev.yaml keda.lagThreshold 에 적용"
echo "------------------------------------------------------------"
