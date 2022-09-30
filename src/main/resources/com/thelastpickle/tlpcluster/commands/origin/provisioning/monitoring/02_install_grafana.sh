#!/usr/bin/env bash
set +o pipefail
set +e
echo "Installing Grafana"

sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
sudo wget -q -O - https://packages.grafana.com/gpg.key | apt-key add -
sudo apt-get update
sudo apt-get install -y grafana

sudo cp config/grafana/grafana.ini /etc/grafana/

mkdir -p /etc/grafana/provisioning/datasources/
sudo cp config/grafana/datasource.yaml /etc/grafana/provisioning/datasources/
sudo chown root:grafana /etc/grafana/provisioning/datasources/datasource.yaml

echo "Adding plugins to Grafana"
sudo grafana-cli plugins install grafana-polystat-panel

echo "Getting and adding Dashboards to Grafana"
source ./mcac-version
DASHBOARDS_FOLDER_NAME="datastax-mcac-dashboards"
GRAFANA_DASHBOARDS_PATH="/var/lib/grafana/dashboards"

sudo mkdir -p ${GRAFANA_DASHBOARDS_PATH}
cd ${GRAFANA_DASHBOARDS_PATH}
curl -LO https://github.com/datastax/metric-collector-for-apache-cassandra/releases/download/v${MCAC_VERSION}/${DASHBOARDS_FOLDER_NAME}-${MCAC_VERSION}.tar.gz
tar -zxf ${DASHBOARDS_FOLDER_NAME}-${MCAC_VERSION}.tar.gz
rm -rf ${DASHBOARDS_FOLDER_NAME}-${MCAC_VERSION}.tar.gz
sudo cp ${DASHBOARDS_FOLDER_NAME}-${MCAC_VERSION}/grafana/generated-dashboards/*.json ${GRAFANA_DASHBOARDS_PATH}
cd -
sudo cp config/grafana/dashboards.yaml /etc/grafana/provisioning/dashboards/
sudo chown -R grafana ${GRAFANA_DASHBOARDS_PATH}

sudo /bin/systemctl daemon-reload
sudo /bin/systemctl enable grafana-server
