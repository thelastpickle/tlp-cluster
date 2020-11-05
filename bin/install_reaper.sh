#!/bin/bash

shopt -s expand_aliases || setopt aliases

export BETA=no
export CLUSTER_NAME=${PWD##*/}
while test $# -gt 0; do
  case "$1" in
    -h|--help)
      echo "install_reaper: automation script for tlp-cluster"
      echo " "
      echo " "
      echo "options:"
      echo "-h, --help                                  show brief help"
      echo "-b                                          Install Reaper latest beta"
      echo "-d                                          Debian package to install"
      echo "--cluster=CLUSTER_NAME                      Cluster on which Reaper should be installed"
      exit 0
      ;;
    -b)
      export BETA=yes
      shift
      ;;
    -d)
      shift
      if test $# -gt 0; then
        export DEBIAN=$1
      else
        echo "no debian package"
        exit 1
      fi
      shift
      ;;
    --cluster*)
      export CLUSTER_NAME=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    *)
      break
      ;;
  esac
done

echo "Installing Reaper on cluster $CLUSTER_NAME..."
LOCAL_DIR=${PWD##*/}
if [[ "$LOCAL_DIR" != "$CLUSTER_NAME" ]];
then
  pushd $CLUSTER_NAME
fi
source env.sh
echo "Installing required dependencies..."
x_all "sudo DEBIAN_FRONTEND=noninteractive apt-get install -y jq" > reaper.log 2>&1

set -x
if [[ "$BETA" == "yes" ]];
then
  # Install the latest beta
  echo "Installing Reaper beta from the repo..."
  x_all "cd provisioning && latest=$(curl https://api.bintray.com/packages/thelastpickle/reaper-deb-beta/cassandra-reaper-beta/versions/_latest|jq -r '.name') && wget https://bintray.com/thelastpickle/reaper-deb-beta/download_file?file_path=reaper_\${latest}_amd64.deb -O reaper_\${latest}_amd64.deb && sudo apt-get install ./reaper_\${latest}_amd64.deb" >> reaper.log 2>&1
else
  if [ -n "$DEBIAN" ];
  then
    # Install the provided debian package
    echo "Installing Reaper from ${DEBIAN}..."
    scp_all $DEBIAN /home/ubuntu
    x_all "sudo apt-get install /home/ubuntu/${DEBIAN##*/}"
  else
    x_all "cd provisioning && latest=$(curl https://api.bintray.com/packages/thelastpickle/reaper-deb/cassandra-reaper/versions/_latest|jq -r '.name') && wget https://bintray.com/thelastpickle/reaper-deb/download_file?file_path=reaper_\${latest}_amd64.deb -O reaper_\${latest}_amd64.deb && sudo apt-get install ./reaper_\${latest}_amd64.deb"  >> reaper.log 2>&1
  fi
fi

x_all "sudo cp /etc/cassandra-reaper/configs/cassandra-reaper-cassandra-sidecar.yaml /etc/cassandra-reaper/cassandra-reaper.yaml"  >> reaper.log 2>&1
x_all 'sudo sed -i "s/contactPoints: \[\"127.0.0.1\"\]/contactPoints: [\"$(hostname)\"]/" /etc/cassandra-reaper/cassandra-reaper.yaml'  >> reaper.log 2>&1

echo "Creating reaper_db keyspace..."
cat << EOF > ./reaper_init.cql
CREATE KEYSPACE IF NOT EXISTS reaper_db with replication = {'class':'SimpleStrategy', 'replication_factor':3};
CREATE TABLE IF NOT EXISTS reaper_db.schema_migration(applied_successful boolean, version int, script_name varchar, script text, executed_at timestamp, PRIMARY KEY (applied_successful, version));
CREATE TABLE IF NOT EXISTS reaper_db.schema_migration_leader(keyspace_name text, leader uuid, took_lead_at timestamp, leader_hostname text, PRIMARY KEY (keyspace_name));
EOF

scp reaper_init.cql cassandra0:/home/ubuntu/provisioning  >> reaper.log 2>&1
c0 "cqlsh \$(hostname) -f provisioning/reaper_init.cql" >> reaper.log 2>&1
echo "Starting Reaper..."
c0 "sudo service cassandra-reaper start && sleep 20" >> reaper.log 2>&1
x_all "sudo service cassandra-reaper start" >> reaper.log 2>&1
echo "Reaper was successfully started on all nodes!"
echo "Reaper is available on all Cassandra nodes with the login 'admin', password 'admin'"
servers=($(cat sshConfig |grep Hostname|cut -d' ' -f3))
echo "  - Reaper:     http://${servers[1]}:8080/webui/"