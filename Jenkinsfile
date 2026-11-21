pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        PUBLISH_BASE = '/var/publish/vinheria'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "=========================================="
                echo " Deploy da Vinheria Agnello - Build #${env.BUILD_NUMBER}"
                echo "=========================================="
                sh '''
                    git --version
                    echo "Commit: $(git rev-parse --short HEAD)"
                    echo "Branch: $(git branch --show-current || echo main)"
                '''
            }
        }

        stage('Build & Validation') {
            steps {
                sh '''
                    echo "[Build] Validando estrutura dos microsservicos Java/Spring Boot..."
                    test -f services/production-service/pom.xml
                    test -f services/tracking-service/pom.xml
                    test -f infra/dnsmasq/dnsmasq.conf
                    test -f infra/gateway/nginx.conf

                    PROD_CLASSES=$(find services/production-service/src -name "*.java" | wc -l)
                    TRACK_CLASSES=$(find services/tracking-service/src -name "*.java" | wc -l)
                    echo "production-service classes Java: ${PROD_CLASSES}"
                    echo "tracking-service classes Java:   ${TRACK_CLASSES}"

                    mkdir -p dist
                    BUILD_TS=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
                    GIT_SHA=$(git rev-parse --short HEAD)

                    cat <<EOF > dist/release-metadata.json
{
  "project": "Vinheria Agnello - Fase 7",
  "buildNumber": "${BUILD_NUMBER}",
  "gitCommit": "${GIT_SHA}",
  "deployedAt": "${BUILD_TS}",
  "services": [
    "production-service (Java 21 / Spring Boot 3.4 - :8081)",
    "tracking-service (Java 21 / Spring Boot 3.4 - :8082)"
  ],
  "infrastructure": {
    "dnsDhcp": "dnsmasq (172.28.0.2 / vinheria.local)",
    "gateway": "nginx TLS 1.2/1.3 (172.28.0.30)"
  }
}
EOF

                    cat <<EOF > dist/index.html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>Vinheria Agnello - Deploy #${BUILD_NUMBER}</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 720px; margin: 40px auto; padding: 0 20px; color: #1f2937; }
    .card { border: 1px solid #d1d5db; border-radius: 8px; padding: 20px; background: #f9fafb; }
    code { background: #e5e7eb; padding: 2px 6px; border-radius: 4px; }
  </style>
</head>
<body>
  <h1>Vinheria Agnello - Publicacao de Producao</h1>
  <div class="card">
    <p><strong>Status:</strong> Publicado com sucesso via Jenkins Pipeline</p>
    <p><strong>Build:</strong> #${BUILD_NUMBER}</p>
    <p><strong>Commit:</strong> <code>${GIT_SHA}</code></p>
    <p><strong>Data (UTC):</strong> ${BUILD_TS}</p>
    <p><strong>Microsservicos:</strong> <code>production-service</code>, <code>tracking-service</code></p>
  </div>
</body>
</html>
EOF
                '''
            }
        }

        stage('DevSecOps Check') {
            steps {
                sh '''
                    echo "[DevSecOps] Executando verificacoes basicas de seguranca..."

                    LEAKED_FILES=$(find . -maxdepth 4 -type f | grep -E '(/\.env$|\.key$|\.pem$)' | wc -l)
                    if [ "$LEAKED_FILES" -ne 0 ]; then
                        echo "ERRO: Arquivos sensiveis encontrados no workspace!"
                        exit 1
                    fi

                    if grep -rnE "BEGIN [A-Z ]*PRIVATE KEY" --exclude="Jenkinsfile" --exclude-dir=".git" .; then
                        echo "ERRO: Chave privada detectada no codigo!"
                        exit 1
                    fi

                    grep -q "JwtAuthFilter" services/production-service/src/main/java/br/com/vinheria/production/JwtAuthFilter.java
                    grep -q "JwtAuthFilter" services/tracking-service/src/main/java/br/com/vinheria/tracking/JwtAuthFilter.java
                    grep -q "TLSv1.3" infra/gateway/nginx.conf
                    echo "[DevSecOps] OK: nenhuma chave privada exposta, JWT e TLSv1.3 verificados."
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    echo "[Deploy] Publicando release #${BUILD_NUMBER} em ${PUBLISH_BASE}..."
                    RELEASE_DIR="${PUBLISH_BASE}/releases/build-${BUILD_NUMBER}"
                    mkdir -p "${RELEASE_DIR}"
                    cp -r dist/* "${RELEASE_DIR}/"
                    cp infra/dnsmasq/dnsmasq.conf "${RELEASE_DIR}/dnsmasq.conf"
                    cp infra/gateway/nginx.conf "${RELEASE_DIR}/nginx.conf"

                    cd "${PUBLISH_BASE}"
                    ln -sfn "releases/build-${BUILD_NUMBER}" current

                    echo "[Deploy] Conteudo publicado em ${PUBLISH_BASE}/current:"
                    ls -la "${PUBLISH_BASE}/current/"
                    echo "Deploy da Vinheria Agnello concluido com sucesso!"
                '''
            }
        }
    }

    post {
        success {
            echo "Pipeline finalizado com status SUCCESS (Build #${env.BUILD_NUMBER})."
        }
        failure {
            echo "Pipeline falhou no Build #${env.BUILD_NUMBER}."
        }
    }
}
