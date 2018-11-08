#!/usr/bin/env bash

cd /cassandra

[[ -d /build ]] && rm -rf /build

mkdir /build

#git checkout-index -a -f --prefix=/build/
rsync -av /cassandra/ /build/

cd /build

export JAVA8_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64

ant

# Install build dependencies and build package
echo "y" | mk-build-deps --install
dpkg-buildpackage -uc -us


ls -lah

cp ../cassandra*all.deb /builds/deb/
cp -R debian/cassandra/etc/cassandra/* /builds/conf/
