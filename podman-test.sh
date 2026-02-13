#!/bin/sh

BASE_URL="http://localhost:8080"

# Step 1: Generate bcrypt hash of "default" password
DEFAULT_TENANT_PASSWORD=$(htpasswd -bnBC 10 "" default | tr -d ':')

# Step 2: Start depot container in background
podman run -d --name depot-test \
    -e DEFAULT_TENANT_PASSWORD="$DEFAULT_TENANT_PASSWORD" \
    -e SPRINGDOC_SWAGGER_UI_ENABLED=true \
    -e SPRINGDOC_API_DOCS_ENABLED=true \
    -p 8080:8080 \
    depot

echo "Waiting for depot to start..."
until curl -sf "$BASE_URL/v3/api-docs" > /dev/null 2>&1; do
    sleep 1
done
echo "Depot is ready."

# Step 3: Get a write token for the default tenant
TOKEN=$(curl -s -X POST "$BASE_URL/admin/register" \
    -H "Content-Type: application/json" \
    -d '{
      "tenant": "default",
      "password": "default",
      "realm": "test",
      "subject": "curl-client",
      "mode": "w",
      "expirationDate": "2040-01-01"
    }' | jq -r '.token')

echo "Token: $TOKEN"

# Step 4: Upload alice29.txt with hash requested
curl -s -X POST "$BASE_URL/put" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@alice29.txt" \
    -F "path=uploads" \
    -F "hash=true" | jq .

# Cleanup
podman stop depot-test
podman rm depot-test
