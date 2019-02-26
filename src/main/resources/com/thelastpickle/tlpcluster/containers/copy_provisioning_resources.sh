#!/bin/sh

set -ex

eval $(ssh-agent)
ssh-add /root/.ssh/aws-private-key

cd /local

parallel-rsync \
    -avrz  \
    -H "${PSSH_HOSTNAMES}" \
    -l ubuntu \
    -O StrictHostKeyChecking=no  \
    -O UserKnownHostsFile=/dev/null \
    ./provisioning/ \
    /home/ubuntu/provisioning/