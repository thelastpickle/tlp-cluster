#!/usr/bin/env bash

# pass either cassandra, stress or monitor to execute all files

echo "Updating local apt database"
apt-get update

if [[ "$1" == "" ]]; then
echo "Pass a provisioning argument please"
exit 1
fi

echo "Running all shell scripts"

# subshell
(
cd $1
for f in $(ls [0-9]*.sh)
do
    bash ${f}
done

echo "Done with shell scripts"
)
