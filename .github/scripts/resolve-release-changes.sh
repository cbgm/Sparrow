#!/usr/bin/env bash
set -euo pipefail

base_sha="${1:-}"
head_sha="${2:-HEAD}"
force_full="${3:-false}"
output_file="${GITHUB_OUTPUT:-/dev/stdout}"
summary_file="${GITHUB_STEP_SUMMARY:-/dev/null}"

android=false
node_registry=false
presence_directory=false
push=false
mailbox=false
federation=false
gateway=false
control_plane_package=false
community_node_package=false
full_build=false

set_all_server() {
  node_registry=true
  presence_directory=true
  push=true
  mailbox=true
  federation=true
  gateway=true
  control_plane_package=true
  community_node_package=true
}

set_full() {
  full_build=true
  android=true
  set_all_server
}

if [[ "${force_full}" == "true" ]] || [[ -z "${base_sha}" ]] || [[ "${base_sha}" =~ ^0+$ ]]; then
  set_full
  changed_files="<full release build>"
else
  changed_files="$(git diff --name-only "${base_sha}" "${head_sha}")"

  while IFS= read -r path; do
    [[ -z "${path}" ]] && continue

    case "${path}" in
      build.gradle.kts|settings.gradle.kts|gradle.properties|gradlew|gradlew.bat|build-logic/*|gradle/*|.github/workflows/release.yml|.github/scripts/resolve-release-changes.sh)
        set_full
        ;;

      androidApp/*|shared/*|core/*|data/*|feature/*|navigation/*|notification/*|resources/*|startup/*)
        android=true
        ;;

      server/protocol/*|server/security/*|server/persistence/*|server/observability/*|server/Dockerfile.runtime)
        set_all_server
        ;;

      server/node-registry/*)
        node_registry=true
        control_plane_package=true
        ;;
      server/presence-directory/*)
        presence_directory=true
        control_plane_package=true
        ;;
      server/push/*)
        push=true
        control_plane_package=true
        ;;
      server/mailbox/*)
        mailbox=true
        community_node_package=true
        ;;
      server/federation/*)
        federation=true
        community_node_package=true
        ;;
      server/gateway/*)
        gateway=true
        community_node_package=true
        ;;

      server/control-plane/*|server/scripts/New-ControlPlaneBundle.ps1|server/scripts/Bootstrap-ControlPlane.Bundle.ps1)
        control_plane_package=true
        ;;
      server/community-node/*|server/scripts/New-CommunityNodeBundle.ps1)
        community_node_package=true
        ;;
    esac
  done <<< "${changed_files}"
fi

any_artifacts=false
if [[ "${android}" == "true" ]] || \
   [[ "${control_plane_package}" == "true" ]] || \
   [[ "${community_node_package}" == "true" ]]; then
  any_artifacts=true
fi

{
  echo "full_build=${full_build}"
  echo "android=${android}"
  echo "node_registry=${node_registry}"
  echo "presence_directory=${presence_directory}"
  echo "push=${push}"
  echo "mailbox=${mailbox}"
  echo "federation=${federation}"
  echo "gateway=${gateway}"
  echo "control_plane_package=${control_plane_package}"
  echo "community_node_package=${community_node_package}"
  echo "any_artifacts=${any_artifacts}"
} >> "${output_file}"

{
  echo "## Release change detection"
  echo
  echo "| Artifact | Build |"
  echo "| --- | --- |"
  echo "| Android debug + signed release APK | ${android} |"
  echo "| node-registry image | ${node_registry} |"
  echo "| presence-directory image | ${presence_directory} |"
  echo "| push image | ${push} |"
  echo "| mailbox image | ${mailbox} |"
  echo "| federation image | ${federation} |"
  echo "| gateway image | ${gateway} |"
  echo "| Control Plane bundle | ${control_plane_package} |"
  echo "| Community Node bundle | ${community_node_package} |"
  echo
  echo "### Changed files"
  echo '```text'
  printf '%s\n' "${changed_files}"
  echo '```'
} >> "${summary_file}"
