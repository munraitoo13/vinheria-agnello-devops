#!/bin/sh
set -e

CERT_DIR="/etc/nginx/certs"
mkdir -p "$CERT_DIR"

if [ ! -f "$CERT_DIR/vinheria.crt" ] || [ ! -f "$CERT_DIR/vinheria.key" ]; then
  echo "[gateway] Generating self-signed TLS certificate for api.vinheria.local..."
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$CERT_DIR/vinheria.key" \
    -out "$CERT_DIR/vinheria.crt" \
    -subj "/C=BR/ST=SP/L=SaoPaulo/O=VinheriaAgnello/OU=DevSecOps/CN=api.vinheria.local" \
    -addext "subjectAltName=DNS:api.vinheria.local,DNS:gateway.vinheria.local,DNS:localhost,IP:172.28.0.30,IP:127.0.0.1"
fi

exec nginx -g "daemon off;"
