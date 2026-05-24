#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://k8s-istiosys-devgwist-75f9e87595-cb185c46783cf9f8.elb.ap-northeast-2.amazonaws.com}"
USER_HOST="${USER_HOST:-user-dev.example.com}"
PASSWORD="${PASSWORD:-Test@1234!}"
PAIRS_FILE="${PAIRS_FILE:-tmp/cognito_test_users_pairs.tsv}"
TARGET_COUNT="${TARGET_COUNT:-100}"

OUT_CSV="${OUT_CSV:-tmp/k6_login_refresh_100.csv}"
OUT_TXT="${OUT_TXT:-tmp/k6_token_list_100.txt}"
OUT_COMMA="${OUT_COMMA:-tmp/k6_token_list_100_comma.txt}"

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required"
  exit 1
fi

if [[ ! -f "$PAIRS_FILE" ]]; then
  echo "ERROR: missing pairs file: $PAIRS_FILE"
  exit 1
fi

mkdir -p "$(dirname "$OUT_CSV")"
echo "email,status,access_token" > "$OUT_CSV"
: > "$OUT_TXT"

ok=0
fail=0

# Use email column, unique
while IFS= read -r email; do
  [[ -z "${email}" ]] && continue
  [[ "$ok" -ge "$TARGET_COUNT" ]] && break

  body="$(jq -nc --arg email "$email" --arg password "$PASSWORD" '{email:$email,password:$password}')"
  status="$(curl -sS -m 20 -o /tmp/k6_login_refresh_resp.json -w "%{http_code}" -X POST \
    -H "Host: ${USER_HOST}" \
    -H "Content-Type: application/json" \
    --data-binary "$body" \
    "${BASE_URL}/api/v2/auth/login" || true)"
  token="$(jq -r '.accessToken // empty' /tmp/k6_login_refresh_resp.json 2>/dev/null || true)"

  if [[ "$status" == "200" && -n "$token" ]]; then
    ok=$((ok + 1))
    echo "$email,$status,$token" >> "$OUT_CSV"
    echo "$token" >> "$OUT_TXT"
  else
    fail=$((fail + 1))
    echo "$email,$status," >> "$OUT_CSV"
  fi
done < <(awk -F '\t' 'NF>=2 && $2!="" {print $2}' "$PAIRS_FILE" | sort -u)

paste -sd, "$OUT_TXT" > "$OUT_COMMA"

count_txt="$(awk 'NF{c++} END{print c+0}' "$OUT_TXT")"
count_comma="$(tr ',' '\n' < "$OUT_COMMA" | awk 'NF{c++} END{print c+0}')"

echo "REFRESH_OK=$ok"
echo "REFRESH_FAIL=$fail"
echo "TOKENS_TXT_COUNT=$count_txt"
echo "TOKENS_COMMA_COUNT=$count_comma"
echo "TOKENS_TXT=$OUT_TXT"
echo "TOKENS_COMMA=$OUT_COMMA"
