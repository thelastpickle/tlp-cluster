set -x
if ls reaper_*.deb 1> /dev/null 2>&1; then
    echo "Reaper was already installed using the provided deb package"
else
    echo "Installing Reaper stable from the repo"
    echo "deb https://dl.bintray.com/thelastpickle/reaper-deb wheezy main" | sudo tee -a /etc/apt/sources.list
    sudo APT_KEY_DONT_WARN_ON_DANGEROUS_USAGE=true DEBIAN_FRONTEND=noninteractive apt-key adv --no-tty --keyserver keyserver.ubuntu.com --recv-keys 2895100917357435 || echo "Key installation may have failed..."
    sudo DEBIAN_FRONTEND=noninteractive apt-get update
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y reaper
fi
cp /etc/cassandra-reaper/configs/cassandra-reaper-cassandra-sidecar.yaml /etc/cassandra-reaper/cassandra-reaper.yaml
sudo sed -i "s/contactPoints: \[\"127.0.0.1\"\]/contactPoints: [\"$(hostname)\"]/" /etc/cassandra-reaper/cassandra-reaper.yaml