#!/usr/bin/env bash
set -euo pipefail

# Example:
#   ./scripts/run_k6_stages.sh
# Optional env:
#   TOKEN_LIST, TOKEN_LIST_FILE, STAGES, BASE_URL, HOST_PRODUCT, HOST_CART, HOST_ORDER, HOST_PAYMENT
#   ITERATIONS, WAVE_GAP_SEC, MAX_DURATION, OUT_DIR

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${OUT_DIR:-$REPO_ROOT/tmp}"

TOKEN_LIST_FILE="${TOKEN_LIST_FILE:-$OUT_DIR/k6_token_list_comma.txt}"
TOKEN_LIST="${TOKEN_LIST:-}"
STAGES="${STAGES:-15 30 50 100}"

BASE_URL="${BASE_URL:-http://k8s-istiosys-devgwist-75f9e87595-cb185c46783cf9f8.elb.ap-northeast-2.amazonaws.com}"
HOST_PRODUCT="${HOST_PRODUCT:-product-dev.example.com}"
HOST_CART="${HOST_CART:-cart-dev.example.com}"
HOST_ORDER="${HOST_ORDER:-order-dev.example.com}"
HOST_PAYMENT="${HOST_PAYMENT:-payment-dev.example.com}"

ITERATIONS="${ITERATIONS:-1}"
WAVE_GAP_SEC="${WAVE_GAP_SEC:-5}"
MAX_DURATION="${MAX_DURATION:-10m}"

mkdir -p "$OUT_DIR"

if [[ -z "$TOKEN_LIST" ]]; then
  if [[ ! -f "$TOKEN_LIST_FILE" ]]; then
    echo "ERROR: TOKEN_LIST is empty and TOKEN_LIST_FILE not found: $TOKEN_LIST_FILE"
    exit 1
  fi
  TOKEN_LIST="$(cat "$TOKEN_LIST_FILE")"
fi

if [[ -z "$TOKEN_LIST" ]]; then
  echo "ERROR: TOKEN_LIST is empty"
  exit 1
fi

for vus in $STAGES; do
  echo
  echo "=== Run stage VUS=${vus} ==="

  log_file="$OUT_DIR/k6_run_vus_${vus}.log"
  summary_txt="$OUT_DIR/k6_summary_vus_${vus}.txt"
  summary_json="$OUT_DIR/k6_summary_vus_${vus}.json"

  docker run --rm -i \
    -v "$REPO_ROOT:/work" \
    -w /work \
    -e TOKEN_LIST="$TOKEN_LIST" \
    -e BASE_URL="$BASE_URL" \
    -e HOST_PRODUCT="$HOST_PRODUCT" \
    -e HOST_CART="$HOST_CART" \
    -e HOST_ORDER="$HOST_ORDER" \
    -e HOST_PAYMENT="$HOST_PAYMENT" \
    -e VUS="$vus" \
    -e ITERATIONS="$ITERATIONS" \
    -e WAVE_GAP_SEC="$WAVE_GAP_SEC" \
    -e MAX_DURATION="$MAX_DURATION" \
    grafana/k6:latest run /work/k6_e2e_flow.js | tee "$log_file"

  cp "$REPO_ROOT/k6_e2e_summary.txt" "$summary_txt"
  cp "$REPO_ROOT/k6_e2e_summary.json" "$summary_json"

  echo "Saved: $summary_txt"
  echo "Saved: $summary_json"
done

echo
echo "All stages completed."

