#!/usr/bin/env bash

# pass either cassandra or stress to execute all files

# Create this policy to stop Cassandra from automatically starting when installed
echo "Setting up policy to disable Cassandra starting automatically"

cat > ./policy-rc.d << EOF
#!/bin/sh
echo "All runlevel operations denied by policy" >&2
exit 101
EOF

POLICY_RC_PATH="/usr/sbin/policy-rc.d"

sudo mv policy-rc.d ${POLICY_RC_PATH}
sudo chown root:root ${POLICY_RC_PATH}
sudo chmod 755 ${POLICY_RC_PATH}

echo "Adding host entry to make no IP set work"


# nevermind this...
#if grep -q "TLP-CLUSTER" /etc/hosts; then
#    echo "Hosts already set up"
#else
#    HOSTENTRY="$(curl http://169.254.169.254/latest/meta-data/local-ipv4) $(hostname)"
#    printf "\n$HOSTENTRY # TLP-CLUSTER \n\n" | sudo tee -a /etc/hosts
#fi



echo "Running all shell scripts"

for f in $(ls [0-9]*.sh)
do
    sh ${f}
done

echo "Done with shell scripts"

echo "Installing all deb packages"

apt-get update

for d in $(ls *.deb)
do
    apt-get install -y ./${d}
done

echo "Finished installing deb packages, deploying cassandra configs files"

sudo cp cassandra/* /etc/cassandra/

export PRIVATE_IP="$(curl http://169.254.169.254/latest/meta-data/local-ipv4)"

echo "Fixing yaml to use private address for stuff instead of localhost"

CASSANDRA_YAML_SETTINGS="$( cat <<EOF
    listen_address: $PRIVATE_IP
    rpc_address:$PRIVATE_IP
    broadcast_address:$PRIVATE_IP

EOF
)"

# replace anything _address with this ip
# messing with my regex will only bring you pain
sed -i -e "s/^\([^#]*_address:\).*/\1 ${PRIVATE_IP} /g" /etc/cassandra/cassandra.yaml


sudo chown -R cassandra:cassandra /var/lib/cassandra