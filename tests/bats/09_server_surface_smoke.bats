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

@test "server help works" {
    run_lucli server --help
    assert_help_exit_code
    assert_output_contains "server"
}

@test "server list works" {
    run_lucli server list
    assert_success
    assert_output_contains "server instances"
}

@test "server monitor help works" {
    run_lucli server monitor --help
    assert_help_exit_code
    assert_output_contains "monitor"
}

@test "binary and jar versions match" {
    local jar_version
    local binary_version

    run_lucli --version
    assert_success
    jar_version="$(printf '%s\n' "${output}" | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+' | head -n 1)"
    [ -n "${jar_version}" ]

    run_lucli_binary --version
    assert_success
    binary_version="$(printf '%s\n' "${output}" | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+' | head -n 1)"
    [ -n "${binary_version}" ]

    [ "${jar_version}" = "${binary_version}" ]
}
