#!/usr/bin/env bash

# Mount the volume that is going to be used to store the data
echo "Mounting NVMe volume that will store the data"

# You might need to edit this device
TARGET_DEVICE=/dev/nvme1n1

# leave this so we don't have to change the config
MOUNT_POINT=/var/lib/cassandra/

sudo mkfs.ext4 -F ${TARGET_DEVICE}

sudo mkdir -p ${MOUNT_POINT}
echo "${TARGET_DEVICE} ${MOUNT_POINT} ext4 defaults,nodev,nosuid 0 2" | sudo tee -a /etc/fstab
sudo mount -a

# Moved the chown mount point to the install script, needs to run after the deb package is installed
# sudo chown -R cassandra:cassandra ${MOUNT_POINT}
sudo chmod 755 ${MOUNT_POINT}
sudo rm -fr ${MOUNT_POINT}/lost+found/
