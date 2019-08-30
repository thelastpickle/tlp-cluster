#!/bin/sh

set -ex
eval $(ssh-agent)
ssh-add /root/.ssh/aws-private-key

cd /local

echo "Starting parallel rsync"

prsync \
    -avrz  \
    -H "${PSSH_HOSTNAMES}" \
    -l ubuntu \
    -O StrictHostKeyChecking=no  \
    -O UserKnownHostsFile=/dev/null \
    ./provisioning/ \
    /home/ubuntu/provisioning/