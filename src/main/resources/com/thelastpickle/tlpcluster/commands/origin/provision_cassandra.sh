#!/bin/sh

eval $(ssh-agent)
ssh-add /root/.ssh/aws-private-key

cd /local

parallel-ssh \
    -ivl ubuntu \
    -O StrictHostKeyChecking=no \
    -O UserKnownHostsFile=/local/known_hosts \
    -h /local/hosts.txt \
    "cd provisioning; chmod +x install.sh; sudo ./install.sh cassandra"