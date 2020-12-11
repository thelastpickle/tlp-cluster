#!/usr/bin/env bash

set +o pipefail
set +e

echo "Configuring Cassandra monitoring"

source ../monitoring/mcac-version
EXPORTER_AGENT_NAME="datastax-mcac-agent"
CASSANDRA_LIB="/usr/share/cassandra/lib"


cd /usr/share/
curl -LO https://github.com/datastax/metric-collector-for-apache-cassandra/releases/download/v${MCAC_VERSION}/${EXPORTER_AGENT_NAME}-${MCAC_VERSION}.tar.gz
tar -zxf ${EXPORTER_AGENT_NAME}-${MCAC_VERSION}.tar.gz
rm -rf ${EXPORTER_AGENT_NAME}-${MCAC_VERSION}.tar.gz


echo "MCAC_ROOT=\"/usr/share/${EXPORTER_AGENT_NAME}-${MCAC_VERSION}\"" | sudo tee -a /etc/cassandra/cassandra-env.sh
echo "JVM_OPTS=\"\$JVM_OPTS -javaagent:\${MCAC_ROOT}/lib/${EXPORTER_AGENT_NAME}.jar\"" | sudo tee -a /etc/cassandra/cassandra-env.sh
echo "Finished configuring Cassandra monitoring for the Cassandra process"
