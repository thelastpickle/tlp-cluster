#!/usr/bin/env bash

echo "Installing Grafana"

sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
sudo wget -q -O - https://packages.grafana.com/gpg.key | apt-key add -
sudo apt-get update
sudo apt-get install grafanaß

sudo cp config/grafana/datasource.yaml /etc/grafana/provisioning/datasources/
sudo chown root:grafana /etc/grafana/provisioning/datasources/datasource.yaml