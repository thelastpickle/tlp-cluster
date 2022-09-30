#!/usr/bin/env bash

echo "Deploying cassandra configs files"

if [ -d "/etc/cassandra/" ]; then
  sudo cp ~/provisioning/cassandra/conf/* /etc/cassandra/
else
  sudo cp ~/provisioning/cassandra/conf/* /etc/dse/cassandra/
fi

export PRIVATE_IP="$(curl http://169.254.169.254/latest/meta-data/local-ipv4)"

echo "Fixing yaml to use private ip (${PRIVATE_IP}) for address settings instead of localhost"

CASSANDRA_YAML_SETTINGS="$( cat <<EOF
    listen_address: $PRIVATE_IP
    rpc_address:$PRIVATE_IP
    broadcast_address:$PRIVATE_IP

EOF
)"

# replace anything _address with this ip
# messing with my regex will only bring you pain
if [ -d "/etc/cassandra/" ]; then
  sudo sed -i -e "s/^\([^#]*_address:\).*/\1 ${PRIVATE_IP} /g" /etc/cassandra/cassandra.yaml
else
  sudo sed -i -e "s/^\([^#]*_address:\).*/\1 ${PRIVATE_IP} /g" /etc/dse/cassandra/cassandra.yaml
fi

sudo chown -R cassandra:cassandra /var/lib/cassandra