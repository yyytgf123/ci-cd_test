#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   BASE_URL='http://<gateway>' USER_HOST='user-dev.example.com' ./scripts/create_k6_users.sh
# Optional:
#   COUNT=10 PREFIX='k6user' EMAIL_DOMAIN='example.com' PASSWORD='Aa123456!' CONFIRM_CODE='123456'

BASE_URL="${BASE_URL:-http://k8s-istiosys-devgwist-75f9e87595-cb185c46783cf9f8.elb.ap-northeast-2.amazonaws.com}"
USER_HOST="${USER_HOST:-user-dev.example.com}"
COUNT="${COUNT:-10}"
PREFIX="${PREFIX:-k6user}"
EMAIL_DOMAIN="${EMAIL_DOMAIN:-example.com}"
PASSWORD="${PASSWORD:-Aa123456!}"
ROLE="${ROLE:-USER}"
CONFIRM_CODE="${CONFIRM_CODE:-}"

OUT_DIR="${OUT_DIR:-./tmp}"
mkdir -p "$OUT_DIR"
USERS_CSV="$OUT_DIR/k6_users.csv"
TOKENS_CSV="$OUT_DIR/k6_tokens.csv"
TOKENS_LIST="$OUT_DIR/k6_token_list.txt"

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required"
  exit 1
fi

ts="$(date +%Y%m%d%H%M%S)"
echo "email,password,signup_status,confirm_status,login_status,access_token" > "$USERS_CSV"
: > "$TOKENS_CSV"

request() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  if [[ -n "$body" ]]; then
    curl -sS -m 20 -o /tmp/k6_user_resp.json -w "%{http_code}" -X "$method" \
      -H "Host: $USER_HOST" \
      -H "Content-Type: application/json" \
      --data-binary "$body" \
      "$BASE_URL$path"
  else
    curl -sS -m 20 -o /tmp/k6_user_resp.json -w "%{http_code}" -X "$method" \
      -H "Host: $USER_HOST" \
      "$BASE_URL$path"
  fi
}

for i in $(seq 1 "$COUNT"); do
  email="${PREFIX}-${ts}-${i}@${EMAIL_DOMAIN}"
  nickname="${PREFIX}_${i}"
  phone="010-0000-$(printf "%04d" "$i")"

  signup_body="$(jq -nc \
    --arg email "$email" \
    --arg password "$PASSWORD" \
    --arg nickname "$nickname" \
    --arg phone "$phone" \
    --arg role "$ROLE" \
    '{email:$email,password:$password,nickname:$nickname,phoneNumber:$phone,role:$role}')"

  signup_code="$(request POST "/api/v2/auth/signup" "$signup_body")"
  confirm_code_status="SKIP"
  login_code="SKIP"
  access_token=""

  if [[ "$signup_code" == "201" && -n "$CONFIRM_CODE" ]]; then
    confirm_body="$(jq -nc --arg email "$email" --arg code "$CONFIRM_CODE" '{email:$email,code:$code}')"
    confirm_code_status="$(request POST "/api/v2/auth/confirm" "$confirm_body")"
  fi

  if [[ "$signup_code" == "201" ]]; then
    login_body="$(jq -nc --arg email "$email" --arg password "$PASSWORD" '{email:$email,password:$password}')"
    login_code="$(request POST "/api/v2/auth/login" "$login_body")"
    if [[ "$login_code" == "200" ]]; then
      access_token="$(jq -r '.accessToken // empty' /tmp/k6_user_resp.json)"
      if [[ -n "$access_token" ]]; then
        echo "$email,$access_token" >> "$TOKENS_CSV"
      fi
    fi
  fi

  echo "$email,$PASSWORD,$signup_code,$confirm_code_status,$login_code,$access_token" >> "$USERS_CSV"
  echo "[$i/$COUNT] email=$email signup=$signup_code confirm=$confirm_code_status login=$login_code"
done

if [[ -s "$TOKENS_CSV" ]]; then
  awk -F',' 'NR>1 {print $2}' "$TOKENS_CSV" | paste -sd, - > "$TOKENS_LIST"
else
  : > "$TOKENS_LIST"
fi

echo
echo "Done"
echo "users csv:   $USERS_CSV"
echo "tokens csv:  $TOKENS_CSV"
echo "token list:  $TOKENS_LIST"
echo
echo "Run k6 with token list:"
echo "TOKEN_LIST=\"\$(cat $TOKENS_LIST)\" docker run --rm -i -v \"$(pwd)\":/work -w /work -e TOKEN_LIST -e BASE_URL='$BASE_URL' -e HOST_PRODUCT='product-dev.example.com' -e HOST_CART='cart-dev.example.com' -e HOST_ORDER='order-dev.example.com' -e HOST_PAYMENT='payment-dev.example.com' -e VUS=$COUNT -e ITERATIONS=1 -e WAVE_GAP_SEC=5 grafana/k6:latest run /work/k6_e2e_flow.js"
