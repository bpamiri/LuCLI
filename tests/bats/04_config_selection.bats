#!/usr/bin/env bats

load './test_helper.bash'

setup_file() {
    require_lucli_artifacts
}

setup() {
    setup_lucli_home
}

teardown() {
    cleanup_lucli_home
}

@test "server start dry-run uses default lucee.json when --config is not set" {
    local project_dir
    project_dir="$(mktemp -d "${BATS_TEST_TMPDIR}/alt-config.XXXXXX")"

    cat > "${project_dir}/lucee.json" << 'EOF'
{
  "name": "base-config",
  "port": 8100,
  "webroot": "./webroot-base"
}
EOF

    cat > "${project_dir}/lucee-alt.json" << 'EOF'
{
  "name": "alt-config",
  "port": 8101,
  "webroot": "./webroot-alt",
  "enableLucee": false
}
EOF

    mkdir -p "${project_dir}/webroot-base" "${project_dir}/webroot-alt"

    run_lucli server start --dry-run "${project_dir}"
    assert_success
    assert_output_contains "\"name\" : \"base-config\""
}

@test "server start dry-run honors --config with alternate file" {
    local project_dir
    project_dir="$(mktemp -d "${BATS_TEST_TMPDIR}/alt-config.XXXXXX")"

    cat > "${project_dir}/lucee.json" << 'EOF'
{
  "name": "base-config",
  "port": 8100,
  "webroot": "./webroot-base"
}
EOF

    cat > "${project_dir}/lucee-alt.json" << 'EOF'
{
  "name": "alt-config",
  "port": 8101,
  "webroot": "./webroot-alt",
  "enableLucee": false
}
EOF

    mkdir -p "${project_dir}/webroot-base" "${project_dir}/webroot-alt"

    run_lucli server start --dry-run --config lucee-alt.json "${project_dir}"
    assert_success
    assert_output_contains "\"name\" : \"alt-config\""
    assert_output_contains "\"enableLucee\" : false"
}
