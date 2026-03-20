#!/usr/bin/env bash

# Shared helpers for LuCLI BATS tests.

LUCLI_ROOT_DIR="$(cd "${BATS_TEST_DIRNAME}/../.." && pwd)"
LUCLI_JAR="${LUCLI_ROOT_DIR}/target/lucli.jar"
LUCLI_BINARY="${LUCLI_ROOT_DIR}/target/lucli"

require_lucli_artifacts() {
    if [[ ! -f "${LUCLI_JAR}" ]]; then
        skip "Missing ${LUCLI_JAR}. Build first: mvn package -Pbinary (or ./tests/test-bats.sh)"
    fi

    if [[ ! -x "${LUCLI_BINARY}" ]]; then
        skip "Missing executable ${LUCLI_BINARY}. Build first: mvn package -Pbinary (or ./tests/test-bats.sh)"
    fi

    if ! command -v java >/dev/null 2>&1; then
        skip "java not found in PATH"
    fi

    if ! java -jar "${LUCLI_JAR}" --version >/dev/null 2>&1; then
        skip "LuCLI artifact is not runnable with current Java. Run ./tests/test-bats.sh to apply project SDKMAN env and rebuild if needed."
    fi
}

setup_lucli_home() {
    export LUCLI_HOME
    LUCLI_HOME="$(mktemp -d "${BATS_TEST_TMPDIR}/lucli-home.XXXXXX")"
}

cleanup_lucli_home() {
    if [[ -n "${LUCLI_HOME:-}" && -d "${LUCLI_HOME}" ]]; then
        rm -rf "${LUCLI_HOME}"
    fi
}

run_lucli() {
    run java -jar "${LUCLI_JAR}" "$@"
}

run_lucli_binary() {
    run "${LUCLI_BINARY}" "$@"
}

assert_success() {
    [ "${status}" -eq 0 ]
}

assert_failure() {
    [ "${status}" -ne 0 ]
}

assert_help_exit_code() {
    [ "${status}" -eq 0 ] || [ "${status}" -eq 1 ] || [ "${status}" -eq 2 ]
}

assert_output_contains() {
    local needle="$1"
    [[ "${output}" == *"${needle}"* ]]
}

assert_output_matches() {
    local pattern="$1"
    [[ "${output}" =~ ${pattern} ]]
}

create_env_test_project() {
    local project_dir
    project_dir="$(mktemp -d "${BATS_TEST_TMPDIR}/env-project.XXXXXX")"

    cat > "${project_dir}/lucee.json" << 'EOF'
{
  "name": "env-test",
  "port": 8080,
  "version": "6.2.2.91",
  "jvm": {
    "maxMemory": "512m",
    "minMemory": "128m"
  },
  "admin": {
    "enabled": true,
    "password": ""
  },
  "monitoring": {
    "enabled": true,
    "jmx": {
      "port": 8999
    }
  },
  "environments": {
    "prod": {
      "port": 8090,
      "jvm": {
        "maxMemory": "2048m"
      },
      "admin": {
        "password": "secret123"
      },
      "monitoring": {
        "enabled": false
      },
      "openBrowser": false
    },
    "dev": {
      "port": 8091,
      "monitoring": {
        "enabled": true,
        "jmx": {
          "port": 9000
        }
      }
    },
    "staging": {
      "port": 8092,
      "jvm": {
        "maxMemory": "1024m"
      }
    }
  }
}
EOF

    printf '%s\n' "${project_dir}"
}
