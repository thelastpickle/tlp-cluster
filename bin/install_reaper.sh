#set -x

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
      echo "--cluster=CLUSTER_NAME                      Cluster on which Reaper should be installed"
      exit 0
      ;;
    -b)
      export BETA=yes
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
x_all "sudo DEBIAN_FRONTEND=noninteractive apt-get install -y jq"
echo "Installing Reaper stable from the repo"
if [[ "$BETA" == "yes" ]];
then
  # Install the latest beta
  x_all "cd provisioning && latest=$(curl https://api.bintray.com/packages/thelastpickle/reaper-deb-beta/cassandra-reaper-beta/versions/_latest|jq -r '.name') && wget https://bintray.com/thelastpickle/reaper-deb-beta/download_file?file_path=reaper_\${latest}_amd64.deb -O reaper_\${latest}_amd64.deb && sudo apt-get install ./reaper_\${latest}_amd64.deb"
else
  # Install the latest stable
  x_all "cd provisioning && latest=$(curl https://api.bintray.com/packages/thelastpickle/reaper-deb/cassandra-reaper/versions/_latest|jq -r '.name') && wget https://bintray.com/thelastpickle/reaper-deb/download_file?file_path=reaper_\${latest}_amd64.deb -O reaper_\${latest}_amd64.deb && sudo apt-get install ./reaper_\${latest}_amd64.deb"
fi

x_all "sudo cp /etc/cassandra-reaper/configs/cassandra-reaper-cassandra-sidecar.yaml /etc/cassandra-reaper/cassandra-reaper.yaml"
x_all 'sudo sed -i "s/contactPoints: \[\"127.0.0.1\"\]/contactPoints: [\"$(hostname)\"]/" /etc/cassandra-reaper/cassandra-reaper.yaml'

echo "CREATE KEYSPACE IF NOT EXISTS reaper_db with replication = {'class':'SimpleStrategy', 'replication_factor':3};" > reaper_init.cql
echo "CREATE TABLE IF NOT EXISTS reaper_db.schema_migration(applied_successful boolean, version int, script_name varchar, script text, executed_at timestamp, PRIMARY KEY (applied_successful, version));" >> reaper_init.cql
echo "CREATE TABLE IF NOT EXISTS reaper_db.schema_migration_leader(keyspace_name text, leader uuid, took_lead_at timestamp, leader_hostname text, PRIMARY KEY (keyspace_name));" >> reaper_init.cql
scp reaper_init.cql cassandra0:/home/ubuntu/provisioning
c0 "cqlsh \$(hostname) -f provisioning/reaper_init.cql"
c0 "sudo service cassandra-reaper start && sleep 20"
x_all "sudo service cassandra-reaper start"
echo "Reaper was successfully started on all nodes!"