#!/usr/bin/env bash

set -x
corresp=$(ps -ef|grep stargate-lib|grep -v grep|wc -l)

if [[ $corresp == "1" ]]
then
  echo "Stargate is running already."
else
  pushd ~
  source provisioning/stargate/environment.sh
  nohup ./starctl --cluster-seed $1 --cluster-name "Test Cluster" --cluster-version $(cat ./provisioning/stargate/cassandra_version) --listen $(curl http://169.254.169.254/latest/meta-data/local-ipv4) --simple-snitch &>~/stargate.out &
  sleep 5
  timeout --foreground 90 grep -q "Finished starting bundles" <(tail -f ~/stargate.out) || (echo "Stargate didn't start within timeout." && exit 1)
  popd
fi
