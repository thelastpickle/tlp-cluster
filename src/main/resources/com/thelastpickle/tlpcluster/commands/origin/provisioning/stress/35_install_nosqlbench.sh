#!/usr/bin/env bash
echo "Installing nosqlbench"


wget https://github.com/nosqlbench/nosqlbench/releases/latest/download/nb -O nb
sudo mv nb /usr/local/bin/nb
sudo chmod 755 /usr/local/bin/nb

