#!/bin/bash

shopt -s expand_aliases || setopt aliases
export STORAGE_PROVIDER="s3_us_west_oregon"
export CREDENTIALS="$HOME/.aws/credentials"
export CLUSTER_NAME=${PWD##*/}
export PREFIX=$CLUSTER_NAME
while test $# -gt 0; do
  case "$1" in
    -h|--help)
      echo "install-medusa: automation script for tlp-cluster"
      echo
      echo
      echo "options:"
      echo "-h, --help                                  show brief help"
      echo "-b, --bucket=bucket-name                    S3 storage bucket name"
      echo "-c, --credentials=/path/to/credentials      AWS credentials file"
      echo "-s, --storage-provider=STORAGE_PROVIDER     Libcloud storage provider (s3_us_west_oregon, ...)"
      echo "--cluster=CLUSTER_NAME                      Cluster on which Medusa should be installed"
      echo "-p, --prefix=bucket_prefix                  Prefix for multitenant buckets (defaults to cluster name)"
      echo "--branch=git_branch                         Branch to install from the GitHub repo"
      exit 0
      ;;
    -b)
      shift
      if test $# -gt 0; then
        export BUCKET=$1
      else
        echo "no bucket specified"
        exit 1
      fi
      shift
      ;;
    --bucket*)
      export BUCKET=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -c)
      shift
      if test $# -gt 0; then
        export CREDENTIALS=$1
      else
        echo "no credentials file specified"
        exit 1
      fi
      shift
      ;;
    --credentials*)
      export CREDENTIALS=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -s)
      shift
      if test $# -gt 0; then
        export STORAGE_PROVIDER=$1
      else
        echo "no storage provider specified"
        exit 1
      fi
      shift
      ;;
    --storage-provider*)
      export STORAGE_PROVIDER=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    --cluster*)
      export CLUSTER_NAME=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -p)
      shift
      if test $# -gt 0; then
        export PREFIX=$1
      else
        echo "no prefix specified"
        exit 1
      fi
      shift
      ;;
    --prefix*)
      export PREFIX=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    --branch*)
      export BRANCH=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    *)
      break
      ;;
  esac
done

if [ -z "$BUCKET" ];
then
  echo "Please set a bucket name using '--bucket=<bucket name>'"
  exit 1
fi

echo "Installing Medusa on cluster $CLUSTER_NAME..."
echo "Using $STORAGE_PROVIDER as storage provider from Apache Libcloud"
LOCAL_DIR=${PWD##*/}
if [[ "$LOCAL_DIR" != "$CLUSTER_NAME" ]];
then
  pushd $CLUSTER_NAME
fi
source env.sh
echo "Installing pip3..."
x_all "sudo apt-get install python3-pip cmake openssl libssl-dev zlib1g-dev -y" > medusa.log 2>&1
echo "Installing Medusa..."
x_all "sudo pip3 install cassandra-medusa[s3] --upgrade" >> medusa.log 2>&1

# Create medusa.ini file
echo "[cassandra]" > ./medusa.ini
echo "[storage]" >> ./medusa.ini
echo "storage_provider = ${STORAGE_PROVIDER}" >> ./medusa.ini
echo "bucket_name = ${BUCKET}" >> ./medusa.ini
echo "key_file = /etc/medusa/credentials" >> ./medusa.ini
echo "base_path = /mnt/cassandra-backups" >> ./medusa.ini
echo "max_backup_age = 0" >> ./medusa.ini
echo "max_backup_count = 0" >> ./medusa.ini
echo "transfer_max_bandwidth = 50MB/s" >> ./medusa.ini
echo "concurrent_transfers = 1" >> ./medusa.ini
echo "multi_part_upload_threshold = 104857600" >> ./medusa.ini
x_all "sudo mkdir /etc/medusa"

for i in "${SERVERS[@]}"
do
    echo "Uploading medusa.ini to $i"
    scp ./medusa.ini $i:/home/ubuntu/medusa.ini
    if [[ $STORAGE_PROVIDER == s3* ]];
    then
      echo "Uploading credentials to $i"
      scp $CREDENTIALS $i:/home/ubuntu/credentials  >> medusa.log 2>&1
    fi
done

x_all "sudo mv /home/ubuntu/medusa.ini /etc/medusa"  >> medusa.log 2>&1
if [[ $STORAGE_PROVIDER == s3* ]];
then
  x_all "sudo mv /home/ubuntu/credentials /etc/medusa" >> medusa.log 2>&1
else
  # Setup NFS server on the first Cassandra node
  echo "Setting up NFS server..."
  c0 "sudo apt install nfs-kernel-server -y"  >> medusa.log 2>&1
  servers_private_ips=($(c0 "nodetool status|grep UN|cut -d' ' -f3"))
  rm exports
  touch exports
  for node in "${servers_private_ips[@]}"
  do
    echo "/mnt/cassandra-backups      ${node}(rw,sync,root_squash,subtree_check)" >> exports
  done
  scp ./exports cassandra0:/home/ubuntu/exports  >> medusa.log 2>&1
  c0 "sudo mv /home/ubuntu/exports /etc/exports" >> medusa.log 2>&1
  c0 "sudo mkdir /mnt/cassandra-backups" >> medusa.log 2>&1
  c0 "sudo chmod 777 -R /mnt/cassandra-backups" >> medusa.log 2>&1
  c0 "sudo service nfs-kernel-server start" >> medusa.log 2>&1
  nfs_server_ip=$(c0 "hostname -I" | xargs)

  # Setup the NFS clients and mounts on the other nodes
  echo "Setting up NFS mounts..."
  x_all "sudo apt install nfs-common -y" >> medusa.log 2>&1
  for i in "${SERVERS[@]:1}"
  do
    ssh $i "echo '${nfs_server_ip}:/mnt/cassandra-backups    /mnt/cassandra-backups      nfs       rw,soft,intr,noatime,x-gvfs-show'| sudo tee –a /etc/fstab" >> medusa.log 2>&1
    ssh $i "sudo mkdir /mnt/cassandra-backups" >> medusa.log 2>&1
    x_all "sudo chmod 777 -R /mnt/cassandra-backups" >> medusa.log 2>&1
    ssh $i "sudo mount -a" >> medusa.log 2>&1
  done
  echo "NFS setup done!"
fi

if [ -n "$BRANCH" ];
then
  echo "Installing branch $BRANCH..."
  x_all "sudo pip3 install git+https://github.com/thelastpickle/cassandra-medusa@${BRANCH} --upgrade" >> medusa.log 2>&1
fi

if [ -n "$PREFIX" ];
then
  echo "Applying prefix $PREFIX"
  x_all "echo 'prefix = ${PREFIX}'|sudo tee -a /etc/medusa/medusa.ini" >> medusa.log 2>&1
fi
echo "Medusa was successfully installed on all nodes!"