#!/usr/bin/env bash
# watch_hpa.sh — HPA 스케일링 실시간 모니터링 (포트폴리오 캡처용)
# 사용법:
#   ./watch_hpa.sh              # 2초마다 자동 갱신
#   ./watch_hpa.sh --once       # 한 번만 출력 (스크린샷 캡처)
#   ./watch_hpa.sh --interval 5 # 5초 간격
#   NAMESPACE=prod ./watch_hpa.sh

NAMESPACE="${NAMESPACE:-prod}"
INTERVAL=2
ONCE=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --once)       ONCE=true ;;
    --interval)   INTERVAL="$2"; shift ;;
    --namespace)  NAMESPACE="$2"; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
  shift
done

# ── 색상 ──────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; YELLOW='\033[1;33m'; GREEN='\033[0;32m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

# ── 서비스 → consumer group 매핑 ──────────────────────────────────────────────
declare -A SVC_GROUP=(
  [user-service]="user-service-group"
  [product-service]="product-service-group"
  [order-service]="order-service-group"
  [payment-service]="payment-service-group"
  [cart-service]="cart-service-group"
)

print_snapshot() {
  local NOW
  NOW=$(date '+%Y-%m-%d %H:%M:%S')

  echo ""
  echo -e "${BOLD}${CYAN}══════════════════════════════════════════════════════════════${RESET}"
  echo -e "${BOLD}  HPA 스케일링 현황   namespace: ${NAMESPACE}   ${NOW}${RESET}"
  echo -e "${BOLD}${CYAN}══════════════════════════════════════════════════════════════${RESET}"

  # ── HPA 전체 목록 조회 (한 번만) ─────────────────────────────────────────────
  local HPA_LIST
  HPA_LIST=$(kubectl get hpa -n "${NAMESPACE}" --no-headers 2>/dev/null || true)

  # ── HPA 요약 테이블 ──────────────────────────────────────────────────────────
  echo ""
  echo -e "${BOLD}[ HPA 상태 ]${RESET}"
  printf "${BOLD}%-42s %-6s %-6s %-10s %-s${RESET}\n" 'NAME' 'MIN' 'MAX' 'CURRENT' 'TARGETS'
  echo "─────────────────────────────────────────────────────────────────────────"

  if [[ -z "$HPA_LIST" ]]; then
    echo -e "  ${YELLOW}(HPA 리소스 없음 — Argo CD 배포 확인 필요)${RESET}"
  else
    while IFS= read -r line; do
      local name min max current targets
      name=$(echo "$line" | awk '{print $1}')
      min=$(echo "$line" | awk '{print $5}')
      max=$(echo "$line" | awk '{print $6}')
      current=$(echo "$line" | awk '{print $7}')
      targets=$(echo "$line" | awk '{print $3}')

      local color="$GREEN"
      if [[ "$current" == "$max" ]] 2>/dev/null; then
        color="$RED"
      elif [[ "${current:-0}" -gt "${min:-0}" ]] 2>/dev/null; then
        color="$YELLOW"
      fi

      printf "${color}%-42s %-6s %-6s %-10s %-s${RESET}\n" \
        "$name" "$min" "$max" "$current" "$targets"
    done <<< "$HPA_LIST"
  fi

  # ── Pod 실행 현황 ─────────────────────────────────────────────────────────────
  echo ""
  echo -e "${BOLD}[ Pod 실행 현황 ]${RESET}"
  printf "${BOLD}%-55s %-12s %-s${RESET}\n" 'POD' 'STATUS' 'NODE'
  echo "─────────────────────────────────────────────────────────────────────────"

  local POD_LIST
  POD_LIST=$(kubectl get pods -n "${NAMESPACE}" \
    -o custom-columns='POD:.metadata.name,STATUS:.status.phase,NODE:.spec.nodeName' \
    --no-headers 2>/dev/null || true)

  if [[ -z "$POD_LIST" ]]; then
    echo -e "  ${YELLOW}(실행 중인 Pod 없음)${RESET}"
  else
    echo "$POD_LIST" | sort
  fi

  # ── HPA describe 상세 ────────────────────────────────────────────────────────
  echo ""
  echo -e "${BOLD}[ HPA 메트릭 상세 ]${RESET}"

  if [[ -z "$HPA_LIST" ]]; then
    echo -e "  ${YELLOW}(HPA 없음)${RESET}"
  else
    while IFS= read -r line; do
      local hpa_name
      hpa_name=$(echo "$line" | awk '{print $1}')
      echo ""
      echo -e "${BOLD}── ${hpa_name} ──${RESET}"
      kubectl describe hpa "${hpa_name}" -n "${NAMESPACE}" 2>/dev/null \
        | grep -E "^\s*(Metrics:|Reference:|Min replicas:|Max replicas:|Current Metrics:|cpu|memory|kafka|AbleToScale|ScalingActive|current:|target:|Conditions:)" \
        | sed 's/^/  /' \
        || true
    done <<< "$HPA_LIST"
  fi

  # ── Pod CPU/Memory (top) ──────────────────────────────────────────────────────
  echo ""
  echo -e "${BOLD}[ Pod CPU/Memory ]${RESET}"
  kubectl top pods -n "${NAMESPACE}" --sort-by=cpu 2>/dev/null \
    || echo -e "  ${YELLOW}(metrics-server 미설치 또는 Pod 없음)${RESET}"

  # ── Node 리소스 ───────────────────────────────────────────────────────────────
  echo ""
  echo -e "${BOLD}[ Node 리소스 ]${RESET}"
  kubectl top nodes 2>/dev/null \
    || echo -e "  ${YELLOW}(metrics-server 미설치)${RESET}"

  echo ""
  echo -e "${CYAN}── 범례: ${GREEN}■ MIN${RESET}${CYAN}  ${YELLOW}■ SCALING 중${RESET}${CYAN}  ${RED}■ MAX 도달${RESET}${CYAN} ──────────────────────────${RESET}"
}

# ── Argo CD 배포 상태 체크 안내 ────────────────────────────────────────────────
check_argocd() {
  echo -e "${BOLD}[ Argo CD Application 상태 ]${RESET}"
  kubectl get application -n argocd 2>/dev/null \
    | grep -E "NAME|service-prod" \
    || echo -e "  ${YELLOW}(argocd namespace 접근 불가 또는 Application 없음)${RESET}"
  echo ""
}

# ── 메인 ──────────────────────────────────────────────────────────────────────
if $ONCE; then
  check_argocd
  print_snapshot
else
  echo -e "${BOLD}HPA 모니터링 시작  (Ctrl+C 종료 / 갱신: ${INTERVAL}s / ns: ${NAMESPACE})${RESET}"
  echo ""
  check_argocd
  while true; do
    clear
    print_snapshot
    sleep "${INTERVAL}"
  done
fi
