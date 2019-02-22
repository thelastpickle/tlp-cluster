#!/bin/sh

set -ex

eval $(ssh-agent)
ssh-add /root/.ssh/aws-private-key

cd /local

parallel-ssh \
    -ivl ubuntu \
    -O StrictHostKeyChecking=no \
    -O UserKnownHostsFile=/dev/null \
    $PSSH_HOSTNAMES  \
    ${1}