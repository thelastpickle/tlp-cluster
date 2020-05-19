#!/usr/bin/env bash
set -x
echo "Installing all deb packages"

for d in $(ls *.deb)
do
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y ./${d}
done

echo "Finished installing deb packages"