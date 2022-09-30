#!/usr/bin/env bash
set -x
echo "Installing all deb packages"

sudo apt-get update
for d in $(ls *.deb)
do
    sudo DEBIAN_FRONTEND=noninteractive apt-get -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" install -y ./${d}
done

dse_version=$(echo \\$(ls dse*.deb) |cut -d'_' -f2)
# shellcheck disable=SC2181
if [[ "$?" == 0 ]]; then
  # DSE installation
  if [ ! -f /etc/apt/sources.list.d/datastax.sources.list ]; then
    echo "deb https://debian.datastax.com/enterprise/ stable main" | sudo tee -a /etc/apt/sources.list.d/datastax.sources.list
    curl -L https://debian.datastax.com/debian/repo_key | sudo apt-key add
    sudo apt-get update
  fi

  max_retry=5
  counter=0
  until sudo apt-get -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" install libaio1 dse=$dse_version \
    dse-full=$dse_version \
    dse-libcassandra=$dse_version \
    dse-libgraph=$dse_version \
    dse-libhadoop2-client-native=$dse_version \
    dse-libhadoop2-client=$dse_version \
    dse-liblog4j=$dse_version \
    dse-libsolr=$dse_version \
    dse-libspark=$dse_version \
    dse-libtomcat=$dse_version -y
  do
    sleep 10
    [[ counter -eq $max_retry ]] && echo "Failed!" && exit 1
    ((counter++))
    echo "Trying again. Try #$counter"
  done
fi

echo "Finished installing deb packages"