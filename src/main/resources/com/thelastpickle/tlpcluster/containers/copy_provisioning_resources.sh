#!/bin/sh

eval $(ssh-agent)
ssh-add /root/.ssh/aws-private-key

cd /local

parallel-rsync \
    -avrz  \
    -h hosts.txt -l ubuntu \
    -O StrictHostKeyChecking=no  \
    -O UserKnownHostsFile=/local/known_hosts \
    ./provisioning/ \
    /home/ubuntu/provisioning/