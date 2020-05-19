#!/usr/bin/env bash
set +o pipefail
set +e

sudo DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-8-dbg || sudo apt-get install -f -y