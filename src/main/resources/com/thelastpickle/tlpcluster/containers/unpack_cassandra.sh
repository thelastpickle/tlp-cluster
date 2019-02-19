#!/usr/bin/env bash

mkdir /out
dpkg-deb -xv $1 /out
cp /out/etc/cassandra/* conf/

echo "Changing ownership to $HOST_USER_ID"

chown -R $HOST_USER_ID conf

echo "DONE"