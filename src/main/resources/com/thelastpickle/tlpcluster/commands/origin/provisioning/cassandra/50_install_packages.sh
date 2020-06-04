#!/usr/bin/env bash
set -x
echo "Installing all deb packages"

for d in $(ls *.deb)
do
    sudo DEBIAN_FRONTEND=noninteractive apt-get -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" install -y ./${d}
done

echo "Finished installing deb packages"