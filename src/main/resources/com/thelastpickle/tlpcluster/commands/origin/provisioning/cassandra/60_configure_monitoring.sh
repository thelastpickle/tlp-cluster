#!/usr/bin/env bash

echo "Configuring Cassandra monitoring"

EXPORTER_AGENT_VERSION="0.9.6"
EXPORTER_AGENT_JAR="cassandra-exporter-agent-${EXPORTER_AGENT_VERSION}.jar"
CASSANDRA_LIB="/usr/share/cassandra/lib"

curl -LO https://github.com/instaclustr/cassandra-exporter/releases/download/v${EXPORTER_AGENT_VERSION}/${EXPORTER_AGENT_JAR}

sudo mv ${EXPORTER_AGENT_JAR} ${CASSANDRA_LIB}/

echo "JVM_OPTS=\"\$JVM_OPTS -javaagent:${CASSANDRA_LIB}/${EXPORTER_AGENT_JAR}\"" | sudo tee -a /etc/cassandra/cassandra-env.sh

echo "Finished configuring Cassandra monitoring"