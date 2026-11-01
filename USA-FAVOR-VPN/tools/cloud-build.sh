#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
export JAVA_HOME=/workspace/toolchains/jdk-21.0.7+6
export ANDROID_HOME=/workspace/toolchains/android-sdk
export GRADLE_USER_HOME=/workspace/toolchains/gradle-home
args=()
if [[ -n ${HTTPS_PROXY:-} ]]; then
 mapfile -t proxy_parts < <(python3 - <<'PY'
import os,urllib.parse
p=urllib.parse.urlparse(os.environ['HTTPS_PROXY'])
print(p.hostname or '')
print(p.port or 8080)
PY
 )
 if [[ -n ${proxy_parts[0]} ]]; then
  args+=("-Dhttps.proxyHost=${proxy_parts[0]}" "-Dhttps.proxyPort=${proxy_parts[1]}" "-Dhttp.proxyHost=${proxy_parts[0]}" "-Dhttp.proxyPort=${proxy_parts[1]}")
 fi
fi
./tools/build.sh "${args[@]}" "$@"
