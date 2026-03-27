#!/usr/bin/env bats

load './test_helper.bash'

setup_file() {
    require_lucli_artifacts
}

setup() {
    setup_lucli_home
}

teardown() {
    stop_all_servers_if_possible
    cleanup_lucli_home
}

write_server_config() {
    local project_dir="$1"
    local config_file="$2"
    local server_name="$3"
    local port="$4"

    cat > "${project_dir}/${config_file}" <<EOF
{
  "name": "${server_name}",
  "port": ${port},
  "openBrowser": false
}
EOF
}

create_project_dir() {
    mktemp -d "${BATS_TEST_TMPDIR}/server-lifecycle.XXXXXX"
}

@test "server lifecycle start -> status -> stop by slug works" {
    local project_dir
    local server_name
    local port
    project_dir="$(create_project_dir)"
    server_name="lifecycle-main"
    port="$(find_available_test_port)"
    write_server_config "${project_dir}" "lucee.json" "${server_name}" "${port}"

    run_lucli server start "${project_dir}"
    assert_success
    assert_output_contains "Server started successfully"
    assert_output_contains "${server_name}"

    run_lucli server status --name "${server_name}"
    assert_success
    assert_output_contains "RUNNING"

    run_lucli server stop --name "${server_name}"
    assert_success
    assert_output_contains "stopped successfully"

    run_lucli server status --name "${server_name}"
    assert_success
    assert_output_contains "NOT RUNNING"
}

@test "server stop --config resolves and stops alternate config slug" {
    local project_dir
    local server_name
    local port
    project_dir="$(create_project_dir)"
    server_name="lifecycle-alt"
    port="$(find_available_test_port)"
    write_server_config "${project_dir}" "lucee-alt.json" "${server_name}" "${port}"

    run_lucli server start --config lucee-alt.json "${project_dir}"
    assert_success
    assert_output_contains "Server started successfully"
    assert_output_contains "${server_name}"

    run_lucli_in_dir "${project_dir}" server stop --config lucee-alt.json
    assert_success
    assert_output_contains "stopped successfully"
    assert_output_contains "${server_name}"

    run_lucli server status --name "${server_name}"
    assert_success
    assert_output_contains "NOT RUNNING"
}

@test "project-scoped status/stop/prune require disambiguation when multiple slugs share one project" {
    local project_dir
    local server_a
    local server_b
    local port_a
    local port_b
    project_dir="$(create_project_dir)"
    server_a="lifecycle-multi-a"
    server_b="lifecycle-multi-b"
    port_a="$(find_available_test_port)"
    port_b="$(find_available_test_port)"
    while [[ "${port_b}" == "${port_a}" ]]; do
        port_b="$(find_available_test_port)"
    done

    write_server_config "${project_dir}" "lucee-a.json" "${server_a}" "${port_a}"
    write_server_config "${project_dir}" "lucee-b.json" "${server_b}" "${port_b}"

    run_lucli server start --config lucee-a.json "${project_dir}"
    assert_success
    run_lucli server start --config lucee-b.json "${project_dir}"
    assert_success

    run_lucli_in_dir "${project_dir}" server status
    assert_failure
    assert_output_contains "Multiple server instances are associated with project"
    assert_output_contains "${server_a}"
    assert_output_contains "${server_b}"

    run_lucli_in_dir "${project_dir}" server stop
    assert_failure
    assert_output_contains "Multiple server instances are associated with project"
    assert_output_contains "Use --name"

    run_lucli_in_dir "${project_dir}" server prune -f
    assert_failure
    assert_output_contains "Multiple server instances are associated with project"

    run_lucli server stop --name "${server_a}"
    assert_success
    run_lucli server stop --name "${server_b}"
    assert_success
}
