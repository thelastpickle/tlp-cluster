#!/usr/bin/env bash
set +e
set -x

sudo DEBIAN_FRONTEND=noninteractive apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-8-dbg