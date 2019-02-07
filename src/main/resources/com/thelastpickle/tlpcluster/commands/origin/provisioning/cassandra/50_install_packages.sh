#!/usr/bin/env bash

echo "Installing all deb packages"

for d in $(ls *.deb)
do
    apt-get install -y ./${d}
done

echo "Finished installing deb packages"