if ls reaper_*.deb 1> /dev/null 2>&1; then
    echo "Reaper was already installed using the provided deb package"
else
    echo "Installing Reaper latest beta from the repo"
    echo "deb https://dl.bintray.com/thelastpickle/reaper-deb-beta wheezy main" | sudo tee -a /etc/apt/sources.list
    sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 2895100917357435
    sudo apt-get update
    sudo apt-get install -y reaper
fi
cp /etc/cassandra-reaper/configs/cassandra-reaper-cassandra-sidecar.yaml /etc/cassandra-reaper/cassandra-reaper.yaml
sudo sed -i "s/contactPoints: \[\"127.0.0.1\"\]/contactPoints: [\"$(hostname)\"]/" /etc/cassandra-reaper/cassandra-reaper.yaml