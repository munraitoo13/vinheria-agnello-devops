#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CAP_DIR="${SCRIPT_DIR}/../captures"
mkdir -p "${CAP_DIR}"

capture_scenario() {
  local mode="$1"
  local port="$2"
  local outfile="$3"
  local container_name="vinheria-tcpdump-${mode}"

  docker rm -f "${container_name}" >/dev/null 2>&1 || true

  echo "[capture] Iniciando captura ${mode} (${outfile}) na porta ${port} de vinheria-gateway..."
  docker run -d --name "${container_name}" \
    --net=container:vinheria-gateway \
    -v "${CAP_DIR}:/captures" \
    alpine:3.21 sh -c "apk add -q --no-cache tcpdump >/dev/null && exec tcpdump -i eth0 -U -s 0 -w /captures/${outfile} tcp port ${port}" >/dev/null

  for _ in $(seq 1 30); do
    if docker exec "${container_name}" pgrep -x tcpdump >/dev/null 2>&1; then
      break
    fi
    sleep 0.5
  done
  sleep 0.5

  echo "[capture] Gerando trafego real de autenticacao e registro de lote via ${mode}..."
  docker run --rm --network vinheria-net --dns 172.28.0.2 alpine:3.21 sh -c "
    apk add -q --no-cache curl >/dev/null
    PROTO='${mode}'
    HOST='gateway.vinheria.local'
    RESP=\$(curl -sk -X POST \"\${PROTO}://\${HOST}/auth/token\" \
      -H 'Content-Type: application/json' \
      -d '{\"username\":\"enologo\",\"password\":\"safra2026\"}')
    TOKEN=\$(echo \"\$RESP\" | sed -n 's/.*\"token\":\"\\([^\"]*\\)\".*/\\1/p')
    curl -sk -X POST \"\${PROTO}://\${HOST}/api/batches\" \
      -H \"Authorization: Bearer \${TOKEN}\" \
      -H 'Content-Type: application/json' \
      -d '{\"variety\":\"Pinot Noir (${mode})\",\"harvestYear\":2026,\"volumeLiters\":1800}' >/dev/null
    curl -sk \"\${PROTO}://\${HOST}/api/tracking/AGN-2026-001\" \
      -H \"Authorization: Bearer \${TOKEN}\" >/dev/null
  "

  sleep 1
  docker kill --signal=INT "${container_name}" >/dev/null 2>&1 || true
  docker wait "${container_name}" >/dev/null 2>&1 || true
  docker rm -f "${container_name}" >/dev/null 2>&1 || true
  ls -lh "${CAP_DIR}/${outfile}"
}

capture_scenario "http" "80" "http-inseguro.pcap"
capture_scenario "https" "443" "https-seguro.pcap"

echo ""
echo "[capture] Capturas finalizadas em ${CAP_DIR}:"
ls -lh "${CAP_DIR}"/*.pcap
