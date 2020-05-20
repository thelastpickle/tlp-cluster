#set -x

shopt -s expand_aliases || setopt aliases

export STORAGE_PROVIDER="s3_us_west_oregon"
export CREDENTIALS="$HOME/.aws/credentials"
export CLUSTER_NAME=${PWD##*/}
while test $# -gt 0; do
  case "$1" in
    -h|--help)
      echo "install-medusa: automation script for tlp-cluster"
      echo " "
      echo " "
      echo "options:"
      echo "-h, --help                                  show brief help"
      echo "-b, --bucket=bucket-name                    S3 storage bucket name"
      echo "-c, --credentials=/path/to/credentials      AWS credentials file"
      echo "-s, --storage-provider=CASSANDRA_VERSION    Libcloud storage provider (s3_us_west_oregon, ...)"
      echo "--cluster=CLUSTER_NAME                      Cluster on which Medusa should be installed"
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
x_all "sudo apt-get install python3-pip -y"
x_all "sudo pip3 install cassandra-medusa[s3] --upgrade"

# Create medusa.ini file
echo "[cassandra]" > ./medusa.ini
echo "[storage]" >> ./medusa.ini
echo "storage_provider = ${STORAGE_PROVIDER}" >> ./medusa.ini
echo "bucket_name = ${BUCKET}" >> ./medusa.ini
echo "key_file = /etc/medusa/credentials" >> ./medusa.ini
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
    echo "Uploading credentials to $i"
    scp $CREDENTIALS $i:/home/ubuntu/credentials
done

x_all "sudo mv /home/ubuntu/medusa.ini /etc/medusa"
x_all "sudo mv /home/ubuntu/credentials /etc/medusa"

echo "Medusa was successfully installed on all nodes!"