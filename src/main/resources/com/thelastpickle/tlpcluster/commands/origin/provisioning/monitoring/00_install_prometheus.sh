#!/usr/bin/env bash
set +o pipefail
set +e

echo "Installing Prometheus"

sudo useradd --no-create-home --shell /bin/false prometheus

ETC_DIR=/etc/prometheus
LIB_DIR=/var/lib/prometheus
BIN_DIR=/usr/local/bin/

sudo mkdir -p ${ETC_DIR}
sudo mkdir -p ${LIB_DIR}

PACKAGE_VERSION=2.17.1
PACKAGE_NAME=prometheus-${PACKAGE_VERSION}.linux-amd64
ARCHIVE_NAME=${PACKAGE_NAME}.tar.gz

curl -LO https://github.com/prometheus/prometheus/releases/download/v${PACKAGE_VERSION}/${ARCHIVE_NAME}

tar xvf ${ARCHIVE_NAME}

sudo cp ${PACKAGE_NAME}/prometheus ${BIN_DIR}
sudo cp ${PACKAGE_NAME}/promtool ${BIN_DIR}

sudo cp -r ${PACKAGE_NAME}/consoles ${ETC_DIR}
sudo cp -r ${PACKAGE_NAME}/console_libraries ${ETC_DIR}

# all configs - including MCAC
echo "Configuring Prometheus for MCAC"
source ./mcac-version
curl -o config/prometheus/mcac.yml -L https://raw.githubusercontent.com/datastax/metric-collector-for-apache-cassandra/v${MCAC_VERSION}/dashboards/prometheus/prometheus.yaml
# pick lines relative to job_name, remove possible next "job_name" line that could have been included, then remove 2 white spaces
sed -i -n '/job_name:.*mcac/,/job_name:.*/p' config/prometheus/mcac.yml
sed -i '${/job_name:/d;}' config/prometheus/mcac.yml
sed -i 's/^  //g' config/prometheus/mcac.yml

sudo cp -f config/prometheus/*.yml ${ETC_DIR}/
sudo cp -f config/prometheus/tg_mcac.json ${ETC_DIR}/
sudo cp -f config/prometheus/prometheus.service /etc/systemd/system/

# Append mcac scrape config to prometheus.yml
cat "config/prometheus/mcac.yml" | sudo tee -a "${ETC_DIR}/prometheus.yml"
# remove now useless yml file
sudo rm -rf "${ETC_DIR}/mcac.yml"

sudo chown -R prometheus:prometheus ${ETC_DIR}
sudo chown -R prometheus:prometheus ${LIB_DIR}

rm -rf ${ARCHIVE_NAME} ${PACKAGE_NAME}
