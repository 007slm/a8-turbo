#!/bin/bash
# Auto-submit SeaTunnel jobs to the Zeta master running inside docker-compose.
# The script waits for the master RPC port, synthesizes a Hazelcast client config
# that points to the master endpoint, submits every job config found under
# $SEATUNNEL_JOB_DIR, and finally tails the submission log to keep the container alive.

set -euo pipefail

MASTER_HOST="${SEATUNNEL_MASTER_HOST:-seatunnel-master}"
MASTER_RPC_PORT="${SEATUNNEL_MASTER_RPC_PORT:-5801}"
CLUSTER_NAME="${SEATUNNEL_CLUSTER_NAME:-seatunnel}"
JOB_DIR="${SEATUNNEL_JOB_DIR:-/config}"
SUBMIT_LOG="${SEATUNNEL_SUBMIT_LOG:-/opt/seatunnel/logs/submitter.log}"
CLIENT_CONFIG_PATH="${SEATUNNEL_CLIENT_CONFIG:-/tmp/hazelcast-client.yaml}"
WAIT_TIMEOUT="${SEATUNNEL_MASTER_WAIT_TIMEOUT_SECONDS:-180}"
WAIT_INTERVAL="${SEATUNNEL_MASTER_WAIT_INTERVAL_SECONDS:-3}"

mkdir -p "$(dirname "${SUBMIT_LOG}")"
touch "${SUBMIT_LOG}"

trim() {
  local var="${1:-}"
  var="${var#"${var%%[![:space:]]*}"}"
  var="${var%"${var##*[![:space:]]}"}"
  printf '%s' "${var}"
}

log() {
  local timestamp
  timestamp="$(date +'%Y-%m-%dT%H:%M:%S%z')"
  printf '%s %s\n' "${timestamp}" "$*" | tee -a "${SUBMIT_LOG}"
}

probe_master() {
  timeout 1 bash -c "cat < /dev/null > /dev/tcp/${MASTER_HOST}/${MASTER_RPC_PORT}" >/dev/null 2>&1
}

wait_for_master() {
  local waited=0
  log "Waiting for SeaTunnel master at ${MASTER_HOST}:${MASTER_RPC_PORT}"
  until probe_master; do
    if (( waited >= WAIT_TIMEOUT )); then
      log "SeaTunnel master not reachable after ${WAIT_TIMEOUT}s"
      return 1
    fi
    sleep "${WAIT_INTERVAL}"
    waited=$((waited + WAIT_INTERVAL))
  done
  log "SeaTunnel master is reachable at ${MASTER_HOST}:${MASTER_RPC_PORT}"
}

generate_client_config() {
  cat > "${CLIENT_CONFIG_PATH}" <<EOF
hazelcast-client:
  cluster-name: seatunnel
  properties:
    hazelcast.logging.type: log4j2
  connection-strategy:
    connection-retry:
      cluster-connect-timeout-millis: 3000
  network:
    cluster-members:
      - ${MASTER_HOST}:${MASTER_RPC_PORT}
EOF
  export HAZELCAST_CLIENT_CONFIG="${CLIENT_CONFIG_PATH}"
  log "Hazelcast client config generated at ${CLIENT_CONFIG_PATH}"
}

job_env_file() {
  local job_file="$1"
  printf '%s' "${job_file%.*}.env"
}

collect_job_variables() {
  local env_file="$1"
  while IFS='=' read -r raw_key raw_value || [[ -n "${raw_key}" ]]; do
    local key
    key="$(trim "${raw_key%%$'\r'*}")"
    local value="${raw_value-}"
    value="${value%%$'\r'*}"
    value="$(trim "${value}")"
    if [[ -z "${key}" || "${key}" == \#* ]]; then
      continue
    fi
    if [[ "${value}" == \"*\" && "${value}" == *\" && "${#value}" -ge 2 ]]; then
      value="${value:1:-1}"
    elif [[ "${value}" == \'*\' && "${value}" == *\' && "${#value}" -ge 2 ]]; then
      value="${value:1:-1}"
    fi
    printf '%s=%s\n' "${key}" "${value}"
  done < "${env_file}"
}

extract_job_name() {
  local job_file="$1"
  local raw_line job_name
  raw_line="$(grep -E '^\s*job\.name\s*=' "${job_file}" | head -n 1 || true)"
  if [[ -n "${raw_line}" ]]; then
    job_name="$(echo "${raw_line}" | awk -F'=' '{print $2}' | tr -d ' "')" || true
  fi
  if [[ -z "${job_name:-}" ]]; then
    job_name="$(basename "${job_file}")"
  fi
  printf '%s' "${job_name}"
}

submit_job() {
  local job_file="$1"
  local job_name
  job_name="$(extract_job_name "${job_file}")"
  local env_file
  env_file="$(job_env_file "${job_file}")"
  local -a job_variables=()
  if [[ -f "${env_file}" ]]; then
    log "Loading submit variables from ${env_file}"
    while IFS= read -r line; do
      job_variables+=("${line}")
    done < <(collect_job_variables "${env_file}")
  elif grep -q '\${[a-zA-Z0-9_:-]*}' "${job_file}"; then
    log "Skipping ${job_name}: ${job_file} contains placeholders but no ${env_file}"
    return
  fi
  log "Submitting ${job_name} via ${job_file}"
  local -a cmd=(/opt/seatunnel/bin/seatunnel.sh
    --cluster "${CLUSTER_NAME}"
    --config "${job_file}"
    --deploy-mode cluster
    --async
    --name "${job_name}")
  for var in "${job_variables[@]}"; do
    cmd+=(--variable "${var}")
  done
  if "${cmd[@]}" >>"${SUBMIT_LOG}" 2>&1; then
    log "SeaTunnel accepted job ${job_name}"
  else
    log "SeaTunnel submission failed for ${job_name}, see ${SUBMIT_LOG} for details"
  fi
}

tail_logs() {
  local engine_log="/opt/seatunnel/logs/seatunnel-starter-client.log"
  if [[ -f "${engine_log}" ]]; then
    exec tail -n +1 -F "${SUBMIT_LOG}" "${engine_log}"
  else
    exec tail -n +1 -F "${SUBMIT_LOG}"
  fi
}

main() {
  if [[ ! -d "${JOB_DIR}" ]]; then
    log "Job directory ${JOB_DIR} does not exist"
    return 1
  fi

  wait_for_master
  generate_client_config

  shopt -s nullglob
  local job_files=("${JOB_DIR}"/*.conf "${JOB_DIR}"/*.json)
  shopt -u nullglob

  if (( ${#job_files[@]} == 0 )); then
    log "No job configurations found under ${JOB_DIR}; submitter will remain idle"
    tail_logs
  fi

  for job in "${job_files[@]}"; do
    submit_job "${job}"
  done

  log "Job submission completed; streaming logs for visibility"
  tail_logs
}

main "$@"
