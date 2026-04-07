#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPORT_DIR="${ROOT_DIR}/target/site/jacoco"
REPORT_HTML="${REPORT_DIR}/index.html"
REPORT_XML="${REPORT_DIR}/jacoco.xml"
REPORT_CSV="${REPORT_DIR}/jacoco.csv"

if [[ "${CI:-}" != "true" && -f "${ROOT_DIR}/.sdkmanrc" ]]; then
    SDKMAN_JAVA_VERSION="$(grep '^java=' "${ROOT_DIR}/.sdkmanrc" | head -n 1 | cut -d'=' -f2- | xargs || true)"
    if [[ -n "${SDKMAN_JAVA_VERSION}" && -d "${HOME}/.sdkman/candidates/java/${SDKMAN_JAVA_VERSION}" ]]; then
        export JAVA_HOME="${HOME}/.sdkman/candidates/java/${SDKMAN_JAVA_VERSION}"
        export PATH="${JAVA_HOME}/bin:${PATH}"
    fi
fi

echo "🧪 Running unit tests with JaCoCo coverage..."
(
    cd "${ROOT_DIR}"
    mvn clean test jacoco:report "$@"
)

if [[ ! -f "${REPORT_HTML}" ]]; then
    echo "❌ Coverage report was not generated at ${REPORT_HTML}"
    exit 1
fi

echo "✅ Coverage reports generated:"
echo "   HTML: ${REPORT_HTML}"
echo "   XML:  ${REPORT_XML}"
echo "   CSV:  ${REPORT_CSV}"

if command -v open >/dev/null 2>&1; then
    open "${REPORT_HTML}" >/dev/null 2>&1 || true
    echo "🌐 Opened coverage report in your default browser."
fi
