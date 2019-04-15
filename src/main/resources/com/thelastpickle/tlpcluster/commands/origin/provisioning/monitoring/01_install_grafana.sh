#!/usr/bin/env bash

echo "Installing Grafana"

curl -s https://packagecloud.io/install/repositories/grafana/stable/script.deb.sh | sudo bash

sudo add-apt-repository "deb https://packagecloud.io/grafana/stable/debian/ stretch main"
sudo apt-get update
sudo apt-get install -y --allow-unauthenticated grafana

sudo cp config/grafana/datasource.yaml /etc/grafana/provisioning/datasources/
sudo chown root:grafana /etc/grafana/provisioning/datasources/datasource.yaml