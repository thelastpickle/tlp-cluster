#!/bin/sh

eval $(ssh-agent)
ssh-add /root/.ssh/aws-private-key

cd /local

parallel-rsync \
    -avrz  \
    -H $PSSH_HOSTS -l ubuntu \
    -O StrictHostKeyChecking=no  \
    -O UserKnownHostsFile=/local/known_hosts \
    ./provisioning/ \
    /home/ubuntu/provisioning/