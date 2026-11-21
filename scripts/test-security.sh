#!/usr/bin/env bash
set -euo pipefail

GATEWAY_HTTP="http://localhost:8088"
GATEWAY_HTTPS="https://localhost:8443"

echo "=== 1. Acesso sem JWT (esperado: 401 Unauthorized) ==="
curl -sk -i "${GATEWAY_HTTPS}/api/batches" | head -n 12
echo ""

echo "=== 2. Emissao de Token JWT para perfil CLIENTE ==="
CLIENTE_RESP=$(curl -sk -X POST "${GATEWAY_HTTPS}/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"cliente","password":"vinho2026"}')
echo "$CLIENTE_RESP"
CLIENTE_TOKEN=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' <<< "$CLIENTE_RESP")
echo ""

echo "=== 3. Tentativa de criacao de lote com perfil CLIENTE (esperado: 403 Forbidden - RBAC) ==="
curl -sk -i -X POST "${GATEWAY_HTTPS}/api/batches" \
  -H "Authorization: Bearer ${CLIENTE_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"variety":"Syrah","harvestYear":2026,"volumeLiters":1500}' | head -n 12
echo ""

echo "=== 4. Emissao de Token JWT para perfil ENOLOGO ==="
ENOLOGO_RESP=$(curl -sk -X POST "${GATEWAY_HTTPS}/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"enologo","password":"safra2026"}')
echo "$ENOLOGO_RESP"
ENOLOGO_TOKEN=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' <<< "$ENOLOGO_RESP")
echo ""

echo "=== 5. Criacao de lote com perfil ENOLOGO via HTTPS -> Integracao DNS com tracking-service (esperado: 201 Created) ==="
CREATED_RESP=$(curl -sk -X POST "${GATEWAY_HTTPS}/api/batches" \
  -H "Authorization: Bearer ${ENOLOGO_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"variety":"Merlot Reserva","harvestYear":2026,"volumeLiters":4200}')
echo "$CREATED_RESP" | python3 -m json.tool
TRACKING_CODE=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["batch"]["trackingCode"])' <<< "$CREATED_RESP")
echo ""

echo "=== 6. Consulta publica autenticada do lote ${TRACKING_CODE} com perfil CLIENTE (esperado: 200 OK) ==="
curl -sk "${GATEWAY_HTTPS}/api/tracking/${TRACKING_CODE}" \
  -H "Authorization: Bearer ${CLIENTE_TOKEN}" | python3 -m json.tool
