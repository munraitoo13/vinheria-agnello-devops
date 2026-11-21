# Vinheria Agnello — Infraestrutura, CI/CD Jenkins e DevSecOps (Fase 7)

Simulação prática da arquitetura de microsserviços da **Vinheria Agnello** em containers Docker, cobrindo resolução interna de nomes (**DNS**) e alocação dinâmica (**DHCP**), pipeline de entrega contínua no **Jenkins**, autenticação **JWT + RBAC**, criptografia em trânsito (**HTTPS / TLS 1.3**) e análise de tráfego com **Wireshark**.

## Arquitetura e Serviços Escolhidos

Dentre os quatro microsserviços definidos na modelagem arquitetural anterior, esta implementação coloca em execução dois serviços centrais com comunicação síncrona autenticada:

| Serviço / Componente | Tecnologia | IP Interno | Porta | Função |
| --- | --- | --- | --- | --- |
| `dns-dhcp` | Alpine + `dnsmasq` | `172.28.0.2` | `53`, `67/udp` | DNS interno (`*.vinheria.local`) e DHCP (`172.28.0.100–150`) |
| `production-service` | Java 21 + Spring Boot 3.4 | `172.28.0.10` | `8081` | Emissão/validação JWT, RBAC e fechamento de lotes |
| `tracking-service` | Java 21 + Spring Boot 3.4 | `172.28.0.20` | `8082` | Validação JWT, RBAC e registro/consulta de linhagem |
| `gateway` | Nginx 1.27 + OpenSSL | `172.28.0.30` | `8088` (HTTP), `8443` (HTTPS) | Reverse proxy com resolução via `dnsmasq` e terminação TLS |
| `jenkins` | Jenkins LTS (JCasC + Job DSL) | `172.28.0.40` | `8080` | Pipeline CI/CD declarativo e publicação em `./publish` |
| `wireshark` *(profile `inspect`)* | `lscr.io/linuxserver/wireshark` | Dinâmico | `3005` | Inspeção gráfica de pacotes `.pcap` (HTTP vs. HTTPS) |

## Pré-requisitos

- Docker Engine + Docker Compose v2+
- Portas livres no host: `8080`, `8081`, `8082`, `8088`, `8443` (e `3005` para o Wireshark)

## Como Executar

1. *(Opcional)* Copie `.env.example` para `.env` caso queira customizar os valores padrão do laboratório:

```bash
cp .env.example .env
```

2. Suba a infraestrutura principal:

```bash
mkdir -p publish captures
docker compose up -d --build
```

3. Verifique DNS e DHCP (`dnsmasq`):

```bash
./scripts/test-dns-dhcp.sh
```

4. Execute os testes de autenticação JWT, RBAC (`401` / `403` / `201`) e comunicação entre microsserviços via DNS interno:

```bash
./scripts/test-security.sh
```

5. Gere as capturas comparativas de tráfego **HTTP sem criptografia** vs. **HTTPS com TLS 1.3** e abra no Wireshark web (`http://localhost:3005`):

```bash
./scripts/capture-traffic.sh
docker compose --profile inspect up -d wireshark
```

## Pipeline CI/CD no Jenkins

- Acesse `http://localhost:8080` (usuário demo: `admin` / `vinheria2026`, configurável via `JENKINS_ADMIN_PASSWORD`).
- O job `vinheria-agnello-deploy` já é provisionado automaticamente via **Jenkins Configuration as Code (JCasC)** e executa 4 estágios definidos no [`Jenkinsfile`](Jenkinsfile):
  1. `Checkout` — leitura do repositório Git.
  2. `Build & Validation` — validação dos microsserviços Spring Boot e geração do pacote de publicação (`release-metadata.json` e `index.html`).
  3. `DevSecOps Check` — checagem contra exposição de chaves privadas/`.env` e validação de filtros JWT/TLS.
  4. `Deploy` — publicação versionada em `/var/publish/vinheria/releases/build-<N>` com atualização do symlink `current`, acessível via gateway em `/publish/`.
