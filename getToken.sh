#!/bin/sh
#
# Fetch a read-only depot access token.
# Prompts for the tenant password and prints the resulting JWT on stdout.
#
# Override defaults with env vars:
#   HOST, TENANT, REALM, SUBJECT, MODE, EXPIRATION_DATE

set -eu

HOST="${HOST:-http://localhost:8080}"
TENANT="${TENANT:-default}"
REALM="${REALM:-test}"
SUBJECT="${SUBJECT:-reader}"
MODE="${MODE:-r}"
EXPIRATION_DATE="${EXPIRATION_DATE:-$(date -v+30d +%Y-%m-%d 2>/dev/null || date -d '+30 days' +%Y-%m-%d)}"

trap 'stty echo 2>/dev/null || true' EXIT

printf "Password for tenant '%s': " "$TENANT" >&2
stty -echo
IFS= read -r password
stty echo
printf '\n' >&2

escape_json() {
    printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
}

body=$(printf '{"tenant":"%s","password":"%s","realm":"%s","subject":"%s","mode":"%s","expirationDate":"%s"}' \
    "$(escape_json "$TENANT")" \
    "$(escape_json "$password")" \
    "$(escape_json "$REALM")" \
    "$(escape_json "$SUBJECT")" \
    "$(escape_json "$MODE")" \
    "$(escape_json "$EXPIRATION_DATE")")

response=$(curl -sS -X POST "$HOST/admin/register" \
    -H 'Content-Type: application/json' \
    -d "$body")

token=$(printf '%s' "$response" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [ -z "$token" ]; then
    printf 'Failed to obtain token. Response:\n%s\n' "$response" >&2
    exit 1
fi

printf '%s\n' "$token"
