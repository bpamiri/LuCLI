#!/usr/bin/env bats

load './test_helper.bash'

setup_file() {
    require_lucli_artifacts
    setup_lucli_home
}

teardown_file() {
    cleanup_lucli_home
}

create_https_preview_project() {
    local project_dir
    project_dir="$(mktemp -d "${BATS_TEST_TMPDIR}/https-preview.XXXXXX")"

    cat > "${project_dir}/lucee.json" << 'EOF'
{
  "name": "https-preview",
  "port": 8080,
  "version": "6.2.2.91",
  "https": {
    "enabled": true,
    "port": 8443,
    "redirect": true
  }
}
EOF

    printf '%s\n' "${project_dir}"
}

@test "server start dry-run include-lucee shows CFConfig preview" {
    local project_dir
    project_dir="$(create_env_test_project)"

    run_lucli server start --dry-run --include-lucee "${project_dir}"
    assert_success
    assert_output_contains ".CFConfig.json"
}

@test "server start dry-run include-tomcat-server shows server.xml preview" {
    local project_dir
    project_dir="$(create_env_test_project)"

    run_lucli server start --dry-run --include-tomcat-server "${project_dir}"
    assert_success
    assert_output_contains "server.xml"
}

@test "server start dry-run include-tomcat-web shows web.xml preview" {
    local project_dir
    project_dir="$(create_env_test_project)"

    run_lucli server start --dry-run --include-tomcat-web "${project_dir}"
    assert_success
    assert_output_contains "web.xml"
}

@test "server start dry-run include-https-keystore-plan shows keystore details" {
    local project_dir
    project_dir="$(create_https_preview_project)"

    run_lucli server start --dry-run --include-https-keystore-plan "${project_dir}"
    assert_success
    assert_output_contains "keystore"
}

@test "server start dry-run include-https-redirect-rules shows redirect details" {
    local project_dir
    project_dir="$(create_https_preview_project)"

    run_lucli server start --dry-run --include-https-redirect-rules "${project_dir}"
    assert_success
    assert_output_contains "redirect"
}

@test "server start dry-run include-all shows combined previews" {
    local project_dir
    project_dir="$(create_env_test_project)"

    run_lucli server start --dry-run --include-all "${project_dir}"
    assert_success
    assert_output_contains ".CFConfig.json"
    assert_output_contains "server.xml"
    assert_output_contains "web.xml"
}
