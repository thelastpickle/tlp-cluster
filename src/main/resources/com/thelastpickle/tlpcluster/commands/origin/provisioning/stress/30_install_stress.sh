#!/usr/bin/env bash
echo "Installing tlp-stress"


sudo DEBIAN_FRONTEND=noninteractive apt-get install -y jq
echo "Installing tlp-stress stable from the repo"
latest=$(curl https://api.bintray.com/packages/thelastpickle/tlp-tools-deb/tlp-stress/versions/_latest|jq -r '.name')
wget https://bintray.com/thelastpickle/tlp-tools-deb/download_file?file_path=tlp-stress_${latest}_all.deb -O tlp-stress_${latest}_all.deb
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y ./tlp-stress_${latest}_all.deb

