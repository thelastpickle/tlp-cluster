#!/usr/bin/env bash

echo "Installing tlp-stress"

PACKAGE_VERSION=1.0
PACKAGE_NAME=tlp-stress_${PACKAGE_VERSION}_amd64.deb

curl -LO https://s3-us-west-2.amazonaws.com/tlp-public/${PACKAGE_NAME}

sudo apt-get update
sudo apt-get install -y ./${PACKAGE_NAME}
