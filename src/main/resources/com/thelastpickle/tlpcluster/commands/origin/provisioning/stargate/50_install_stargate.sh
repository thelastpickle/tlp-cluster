#!/bin/bash

echo "Installing Stargate"
sudo DEBIAN_FRONTEND=noninteractive apt-get -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" install -y unzip curl
URL=`curl -s "https://api.github.com/repos/stargate/stargate/releases/latest" | grep "browser_download_url" | cut -d '"' -f 4`
wget $URL -O /home/ubuntu/stargate.zip
cd /home/ubuntu && unzip stargate.zip
sudo chown ubuntu:ubuntu -R /home/ubuntu/starctl
sudo chmod u+x /home/ubuntu/starctl
sudo chown ubuntu:ubuntu -R /home/ubuntu/stargate-lib
