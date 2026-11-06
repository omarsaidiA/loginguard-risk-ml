#!/usr/bin/env bash
# Fill a running LoginGuard with demo data through its API (the same way the UI does):
# monitored users, a few weeks of normal logins, and two account takeover scenarios.
#
#   LOGINGUARD_ADMIN_PASSWORD=... scripts/seed-demo.sh [http://localhost:8080]
#
# Prints the risk level of each takeover scenario; exits with an error if one is not HIGH.
set -euo pipefail

base=${1:-http://localhost:8080}
: "${LOGINGUARD_ADMIN_PASSWORD:?set LOGINGUARD_ADMIN_PASSWORD}"
jar=$(mktemp)
trap 'rm -f "$jar"' EXIT

# Session cookie and CSRF token, exactly like the browser app
curl -fsS -c "$jar" -b "$jar" -o /dev/null "$base/api/auth/csrf"
csrf() { awk '$6 == "XSRF-TOKEN" { print $7 }' "$jar"; }
call() {
  local method=$1 path=$2 body=${3:-}
  curl -fsS -c "$jar" -b "$jar" -X "$method" -H "X-XSRF-TOKEN: $(csrf)" \
    -H "Content-Type: application/json" ${body:+--data "$body"} "$base$path"
}

call POST /api/auth/login \
  "{\"username\": \"admin\", \"password\": \"$LOGINGUARD_ADMIN_PASSWORD\"}" >/dev/null

# Placeholder emails on the reserved example.com domain
user_id() { sed -n 's/.*"id":\([0-9]*\).*/\1/p'; }
alice=$(call POST /api/users '{"username": "alice.martin", "email": "alice.martin@example.com", "department": "Finance"}' | user_id)
bruno=$(call POST /api/users '{"username": "bruno.leroy", "email": "bruno.leroy@example.com", "department": "IT"}' | user_id)
chloe=$(call POST /api/users '{"username": "chloe.dubois", "email": "chloe.dubois@example.com", "department": "Sales"}' | user_id)

event() { # user date ip country device successful
  call POST /api/login-events "{\"userId\": $1, \"occurredAt\": \"$2\", \"ipAddress\": \"$3\",
    \"countryCode\": \"$4\", \"deviceId\": \"$5\", \"successful\": $6}"
}
level() { sed -n 's/.*"level":"\([A-Z]*\)".*/\1/p'; }

# Three weeks of ordinary office logins (documentation IP ranges, RFC 5737)
for day in $(seq -w 1 21); do
  event "$alice" "2026-10-${day}T08:1${day: -1}:00Z" 198.51.100.10 FR laptop-alice true >/dev/null
  event "$bruno" "2026-10-${day}T09:0${day: -1}:00Z" 198.51.100.20 FR desktop-bruno true >/dev/null
  event "$chloe" "2026-10-${day}T07:5${day: -1}:00Z" 198.51.100.30 BE phone-chloe true >/dev/null
done
# A mistyped password now and then is normal
event "$bruno" 2026-10-22T09:00:00Z 198.51.100.20 FR desktop-bruno false >/dev/null
normal=$(event "$bruno" 2026-10-22T09:01:00Z 198.51.100.20 FR desktop-bruno true | level)

# Scenario 1: password guessing from abroad at night, then success on an unknown device
for minute in 10 12 14 16 18 20; do
  event "$alice" "2026-10-23T02:${minute}:00Z" 203.0.113.66 RU unknown-host false >/dev/null
done
takeover=$(event "$alice" 2026-10-23T02:25:00Z 203.0.113.66 RU unknown-host true | level)

# Scenario 2: "impossible travel", Belgium then Brazil 40 minutes later, new device
event "$chloe" 2026-10-23T07:50:00Z 198.51.100.30 BE phone-chloe true >/dev/null
travel=$(event "$chloe" 2026-10-23T08:30:00Z 203.0.113.99 BR tablet-unknown true | level)

echo "normal login after one typo: $normal"
echo "password guessing then success: $takeover"
echo "impossible travel: $travel"
[ "$takeover" = HIGH ] && [ "$normal" = LOW ]
