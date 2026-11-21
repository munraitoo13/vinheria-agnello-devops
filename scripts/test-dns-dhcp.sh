#!/usr/bin/env bash
set -euo pipefail

echo "=== 1. Teste de Resolucao DNS via dnsmasq (172.28.0.2) ==="
docker exec vinheria-production sh -c '
  for host in dns.vinheria.local production.vinheria.local tracking.vinheria.local gateway.vinheria.local api.vinheria.local jenkins.vinheria.local; do
    echo "--- nslookup $host 172.28.0.2 ---"
    nslookup "$host" 172.28.0.2 | grep -E "Name:|Address" | tail -n 2
  done
'

echo ""
echo "=== 2. Teste de Handshake DHCP (DISCOVER -> OFFER -> REQUEST -> ACK) ==="
docker run --rm --network vinheria-net --cap-add NET_ADMIN --cap-add NET_RAW --dns 172.28.0.2 \
  alpine:3.21 sh -c 'udhcpc -i eth0 -n -q -t 3 -s /bin/true'

echo ""
echo "=== 3. Leases Ativos em /var/lib/misc/dnsmasq.leases ==="
docker exec vinheria-dns-dhcp cat /var/lib/misc/dnsmasq.leases

echo ""
echo "=== 4. Logs Recentes do dnsmasq (DHCP + DNS Queries) ==="
docker logs vinheria-dns-dhcp --tail 25
