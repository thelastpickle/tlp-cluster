#!/usr/bin/env bash

mkdir /out
dpkg-deb -xv $1 /out
cp /out/etc/cassandra/* conf/
