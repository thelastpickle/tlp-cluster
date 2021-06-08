#!/usr/bin/env bash
set +e

DOWNLOAD_DIR=/home/ubuntu
LIB_DIR=/var/lib/stargate
#BIN_DIR=/usr/bin
SYSTEMD_DIR=/lib/systemd/system

echo "Installing Stargate"
sudo DEBIAN_FRONTEND=noninteractive apt-get -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" install -y unzip curl
URL=`curl -s "https://api.github.com/repos/stargate/stargate/releases/latest" | grep "browser_download_url" | cut -d '"' -f 4`
wget "$URL" -O $DOWNLOAD_DIR/stargate.zip

sudo mkdir -p $LIB_DIR
sudo chown ubuntu:ubuntu $LIB_DIR

#cp -v start.sh $LIB_DIR/
mv -v $DOWNLOAD_DIR/stargate.zip $LIB_DIR/
pushd $LIB_DIR && unzip stargate.zip && rm stargate.zip && popd

sudo chown ubuntu:ubuntu -R $LIB_DIR
sudo chmod u+x $LIB_DIR/starctl
#sudo chmod u+x $LIB_DIR/start.sh

#sudo ln -s $LIB_DIR/start.sh $BIN_DIR/stargate
sudo cp stargate.service $SYSTEMD_DIR
sudo ln -s $SYSTEMD_DIR/stargate.service /etc/systemd/system/stargate.service

# If we are installing on the Stargate seed host then when we start Stargate we should point to the Cassandra seed host.
# Otherwise, if we are a Stargate non-seed host then when we start Stargate we should point to the Stargate seed host.
if [[ "$(grep "STARGATE_SEED" environment.sh | cut -d'=' -f2)" == "$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4)" ]]
then
  echo "export SEED_HOST=\$CASSANDRA_SEED" >> environment.sh
else
  echo "export SEED_HOST=\$STARGATE_SEED" >> environment.sh
fi

echo "export STARGATE_HOME=$LIB_DIR" >> environment.sh
