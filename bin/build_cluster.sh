#!/bin/bash

dir=$(dirname $0)
BASE_CLUSTER_DIR="$(dirname "$dir")"

shopt -s expand_aliases || setopt aliases

export STRESS_NODES=0
export CASSANDRA_VERSION=3.11.8
export CLUSTER_NAME=test_cluster
export GC=CMS
export HEAP=8
export INSTANCES=3
export INSTANCE_TYPE=m3.xlarge
export JDK=8
export BYPASS_PAUSE=n
while test $# -gt 0; do
  case "$1" in
    -h|--help)
      echo "build-cluster: automation script for tlp-cluster"
      echo " "
      echo " "
      echo "options:"
      echo "-h, --help                                  show brief help"
      echo "-n, --name=CLUSTER_NAME                     Cluster name"
      echo "-s, --stress=STRESS_NODES                   specify the number of stress nodes"
      echo "-v, --cassandra-version=CASSANDRA_VERSION   specify the version of Cassandra to install"
      echo "-d, --extra-deb-package=EXTRA_DEB           optional deb package to install on the nodes"
      echo "-c, --cassandra-nodes=3                     number of Cassandra nodes to start (default: 3)"
      echo "-i, --instance-type=r3.2xlarge              Instance type for Cassandra nodes (default: m3.xlarge)"
      echo "--gc=G1                                     GC algorithm to use. Possible values: G1, Shenandoah, CMS, ZGC"
      echo "--heap=8                                    Heap size in GB (8, 16, 32, ...)"
      echo "--jdk=11                                    OpenJDK version to use (8, 11, 14)"
      echo "--cores=<number of cores>                   Number of cores for ConcGCThreads and ParallelGCThreads"
      echo "-y                                          Bypass the pause before running the install phase"
      exit 0
      ;;
    -n)
      shift
      if test $# -gt 0; then
        export CLUSTER_NAME=$1
      else
        echo "no cluster name specified"
        exit 1
      fi
      shift
      ;;
    --name*)
      export CLUSTER_NAME=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -s)
      shift
      if test $# -gt 0; then
        export STRESS_NODES=$1
      else
        echo "no stress nodes specified"
        exit 1
      fi
      shift
      ;;
    --stress*)
      export STRESS_NODES=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -v)
      shift
      if test $# -gt 0; then
        export CASSANDRA_VERSION=$1
      else
        echo "no cassandra version specified"
        exit 1
      fi
      shift
      ;;
    --cassandra-version*)
      export CASSANDRA_VERSION=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -d)
      shift
      if test $# -gt 0; then
        export EXTRA_DEB=$1
      else
        echo "no extra debian package specified"
        exit 1
      fi
      shift
      ;;
    --extra-deb-package*)
      export EXTRA_DEB=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    --gc*)
      export GC=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    --heap*)
      export HEAP=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    --jdk*)
      export JDK=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -c)
      shift
      if test $# -gt 0; then
        export INSTANCES=$1
      else
        echo "no number of instances specified"
        exit 1
      fi
      shift
      ;;
    --cassandra-nodes*)
      export INSTANCES=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -i)
      shift
      if test $# -gt 0; then
        export INSTANCE_TYPE=$1
      else
        echo "no instance type specified"
        exit 1
      fi
      shift
      ;;
    --instance-type*)
      export INSTANCE_TYPE=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    --cores*)
      export GC_CORES=`echo $1 | sed -e 's/^[^=]*=//g'`
      shift
      ;;
    -y)
      export BYPASS_PAUSE=y
      shift
      ;;
    *)
      break
      ;;
  esac
done

mkdir -p $CLUSTER_NAME
pushd $CLUSTER_NAME
tlp-cluster clean
tlp-cluster init $USER $USER-${CLUSTER_NAME} "Test cluster built by $USER: ${CLUSTER_NAME}" --stress $STRESS_NODES \
            -c $INSTANCES --instance $INSTANCE_TYPE --az a
tlp-cluster up --auto-approve
tlp-cluster use $CASSANDRA_VERSION

if [ -z "$EXTRA_DEB" ]
then
    echo "no deb package provided, proceeding with the install phase"
else
    echo "copying $EXTRA_DEB into the provisioning directory and running the install phase"
    cp $EXTRA_DEB ./provisioning/cassandra
fi

cp $BASE_CLUSTER_DIR/bin/build-cluster/jvm.options.template ./provisioning/cassandra/conf/jvm.options
cp $BASE_CLUSTER_DIR/bin/build-cluster/jvm8-server.options.template ./provisioning/cassandra/conf/jvm8-server.options
cp $BASE_CLUSTER_DIR/bin/build-cluster/jvm11-server.options.template ./provisioning/cassandra/conf/jvm11-server.options

export JVM_OPTIONS_FILE="jvm.options"
if [[ $CASSANDRA_VERSION == "4."* ]]
then
  JVM_OPTIONS_FILE="jvm11-server.options"
  if [[ $JDK == "11"* ]]
  then
    cp $BASE_CLUSTER_DIR/bin/build-cluster/20_java_11.sh ./provisioning/cassandra/20_java.sh
  fi
  if [[ $JDK == "14"* ]]
  then
    cp $BASE_CLUSTER_DIR/bin/build-cluster/20_java_14.sh ./provisioning/cassandra/20_java.sh
  fi
fi

CONC_GC_CORES=""
PARALLEL_GC_CORES=""
if [ -n "$GC_CORES" ];
then
  CONC_GC_CORES="-XX:ConcGCThreads=$GC_CORES"
  PARALLEL_GC_CORES="-XX:ParallelGCThreads=$GC_CORES"
fi

if [ "$GC" == "G1" ]
then
  echo "Applying G1 as garbage collector..."
  cat << EOF >> ./provisioning/cassandra/conf/${JVM_OPTIONS_FILE}
-XX:+UseG1GC
-XX:G1RSetUpdatingPauseTimePercent=5
-XX:MaxGCPauseMillis=300
-XX:InitiatingHeapOccupancyPercent=70
-Xms${HEAP}G
-Xmx${HEAP}G
${CONC_GC_CORES}
${PARALLEL_GC_CORES}
EOF
fi

if [ "$GC" == "CMS" ]
then
  echo "Applying CMS as garbage collector..."
  NEW_GEN=$(($HEAP / 2))
  if [[ $CASSANDRA_VERSION != "4."* ]]
  then
    echo "-XX:+UseParNewGC" >> ./provisioning/cassandra/conf/${JVM_OPTIONS_FILE}
  fi
  cat << EOF >> ./provisioning/cassandra/conf/${JVM_OPTIONS_FILE}
-XX:+UseConcMarkSweepGC
-XX:+CMSParallelRemarkEnabled
-XX:SurvivorRatio=8
-XX:MaxTenuringThreshold=1
-XX:CMSInitiatingOccupancyFraction=75
-XX:+UseCMSInitiatingOccupancyOnly
-XX:CMSWaitDuration=10000
-XX:+CMSParallelInitialMarkEnabled
-XX:+CMSEdenChunksRecordAlways
-XX:+CMSClassUnloadingEnabled
-Xms${HEAP}G
-Xmx${HEAP}G
-Xmn${NEW_GEN}G
${CONC_GC_CORES}
${PARALLEL_GC_CORES}
EOF
fi

if [ "$GC" == "Shenandoah" ]
then
  # Set up openjdk with shenandoah
  if [[ $JDK == "8"* ]]
  then
    cp $BASE_CLUSTER_DIR/bin/build-cluster/20_java_shenandoah_8.sh ./provisioning/cassandra/20_java.sh
  fi
  if [[ $JDK == "11"* ]]
  then
    cp $BASE_CLUSTER_DIR/bin/build-cluster/20_java_shenandoah_11.sh ./provisioning/cassandra/20_java.sh
  fi
  if [[ $JDK == "14"* ]]
  then
    cp $BASE_CLUSTER_DIR/bin/build-cluster/20_java_shenandoah_14.sh ./provisioning/cassandra/20_java.sh
  fi
  echo "Applying Shenandoah as garbage collector..."
  cat << EOF >> ./provisioning/cassandra/conf/${JVM_OPTIONS_FILE}
-XX:+UnlockExperimentalVMOptions
-XX:+UseShenandoahGC
-XX:+UseLargePages
-verbose:gc
-Xms${HEAP}G
-Xmx${HEAP}G
${CONC_GC_CORES}
${PARALLEL_GC_CORES}
EOF
fi

if [ "$GC" == "ZGC" ]
then
  echo "Applying ZGC as garbage collector..."
  cat << EOF >> ./provisioning/cassandra/conf/${JVM_OPTIONS_FILE}
-XX:+UnlockExperimentalVMOptions
-XX:+UseZGC
-XX:+UseTransparentHugePages
-verbose:gc
-Xms${HEAP}G
-Xmx${HEAP}G
${CONC_GC_CORES}
${PARALLEL_GC_CORES}
EOF
fi

if [ "$BYPASS_PAUSE" == "n" ]
then
  read -p "Time to adjust the configuration/packages before install starts. Press any key to trigger the install."
fi

success=0
attempts=1
while [ $success -eq  0 ] && [ $attempts -lt 5 ];
do
    echo "Installing packages (attempt $attempts)"
    tlp-cluster install > install.log 2>&1 || echo "meh... install phase seem to have failed"
    grep "try rerunning the install" install.log > /dev/null 2>&1
    success=$? # 0 means we found errors in the logs, so we need to try again
    attempts=$((attempts+1))
done

if [ $success -eq  0 ]
then
    echo "tlp-cluster install failed after $attempts retries. Exiting (╯°□°)╯︵ ┻━┻"
    echo "You'll need to tear down the cluster manually by running the following command from within the ./$CLUSTER_NAME directory: tlp-cluster down --auto-approve"
    exit 1
fi

success=0
attempts=1
while [ $success -eq  0 ]  && [ $attempts -lt 5 ];
do
    echo "Starting Cassandra (attempt $attempts)"
    tlp-cluster start > start.log 2>&1 || echo "meh... start phase seem to have failed"
    tail -3 start.log | grep "Non zero response returned" > /dev/null 2>&1
    success=$?
    attempts=$((attempts+1))
done

if [ $success -eq 0 ]
then
    echo "tlp-cluster start failed after $attempts retries. Exiting (╯°□°)╯︵ ┻━┻"
    echo "You'll need to tear down the cluster manually by running the following command from within the ./$CLUSTER_NAME directory: tlp-cluster down --auto-approve"
    exit 1
fi

tail start.log
