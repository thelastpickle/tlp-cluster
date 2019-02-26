#!/usr/bin/env bash

set -e

echo "HOSTNAMES: ${PSSH_HOSTNAMES}"

cd /local

for host in ${PSSH_HOSTNAMES}
do
    echo "Configuring dashboard for host ${host}"

    curl -X POST http://admin:admin@${host}:3000/api/user/using/1
    echo

    API_RESPONSE=$(curl \
        -X POST \
        -H "Content-Type: application/json" \
        -d '{"name":"adminApiKey", "role": "Admin"}' \
        http://admin:admin@${host}:3000/api/auth/keys)
    echo

    USER_TOKEN=$(echo ${API_RESPONSE} | jq .key -r)

    curl \
        -X POST \
        --insecure \
        -H "Authorization: Bearer ${USER_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "@provisioning/monitoring/config/grafana/request_overview.json" \
        http://${host}:3000/api/dashboards/db
    echo
done