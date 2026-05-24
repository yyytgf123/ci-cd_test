#!/usr/bin/env bash
set -euo pipefail

# Example:
#   ./scripts/login_k6_users.sh
# Optional env:
#   BASE_URL, USER_HOST, START, END, EMAIL_PREFIX, EMAIL_DOMAIN, PASSWORD, OUT_DIR

BASE_URL="${BASE_URL:-http://k8s-istiosys-devgwist-75f9e87595-cb185c46783cf9f8.elb.ap-northeast-2.amazonaws.com}"
USER_HOST="${USER_HOST:-user-dev.example.com}"
START="${START:-1}"
END="${END:-20}"
EMAIL_PREFIX="${EMAIL_PREFIX:-test}"
EMAIL_DOMAIN="${EMAIL_DOMAIN:-test.com}"
PASSWORD="${PASSWORD:-Test@1234!}"
OUT_DIR="${OUT_DIR:-./tmp}"

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required"
  exit 1
fi

mkdir -p "$OUT_DIR"
CSV_FILE="$OUT_DIR/k6_login_result.csv"
TOKEN_LIST_FILE="$OUT_DIR/k6_token_list.txt"
TOKEN_LIST_COMMA_FILE="$OUT_DIR/k6_token_list_comma.txt"

echo "email,status,access_token" > "$CSV_FILE"
: > "$TOKEN_LIST_FILE"

ok=0
fail=0

for i in $(seq "$START" "$END"); do
  email="${EMAIL_PREFIX}${i}@${EMAIL_DOMAIN}"
  body="$(jq -nc --arg email "$email" --arg password "$PASSWORD" '{email:$email,password:$password}')"

  status="$(curl -sS -m 20 -o /tmp/k6_login_resp.json -w "%{http_code}" -X POST \
    -H "Host: ${USER_HOST}" \
    -H "Content-Type: application/json" \
    --data-binary "$body" \
    "${BASE_URL}/api/v2/auth/login")"

  token="$(jq -r '.accessToken // empty' /tmp/k6_login_resp.json)"

  if [[ "$status" == "200" && -n "$token" ]]; then
    ok=$((ok + 1))
    echo "$email,$status,$token" >> "$CSV_FILE"
    echo "$token" >> "$TOKEN_LIST_FILE"
  else
    fail=$((fail + 1))
    echo "$email,$status," >> "$CSV_FILE"
  fi
done

paste -sd, "$TOKEN_LIST_FILE" > "$TOKEN_LIST_COMMA_FILE"

echo "Done: OK=${ok}, FAIL=${fail}"
echo "CSV:   $CSV_FILE"
echo "LIST:  $TOKEN_LIST_FILE"
echo "COMMA: $TOKEN_LIST_COMMA_FILE"

