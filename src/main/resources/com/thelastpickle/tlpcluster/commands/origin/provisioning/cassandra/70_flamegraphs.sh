#!/usr/bin/env bash


sudo bash -c "echo 1 > /proc/sys/kernel/perf_event_paranoid"
sudo bash -c "echo 0 > /proc/sys/kernel/kptr_restrict"

mkdir profiler
cd profiler

wget https://github.com/jvm-profiling-tools/async-profiler/releases/download/v1.5/async-profiler-1.5-linux-x64.tar.gz



tar -zxvf async-profiler*

# sudo ./profiler.sh -d 30 -f /tmp/flamegraph.svg $(cat /var/run/cassandra/cassandra.pid)