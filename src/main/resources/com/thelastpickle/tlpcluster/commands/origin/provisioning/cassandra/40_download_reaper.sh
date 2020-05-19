if ls reaper_*.deb 1> /dev/null 2>&1; then
    echo "Reaper was already installed using the provided deb package"
else
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y jq
    echo "Installing Reaper stable from the repo"
    latest=$(curl https://api.bintray.com/packages/thelastpickle/reaper-deb/cassandra-reaper/versions/_latest|jq -r '.name')
    wget https://bintray.com/thelastpickle/reaper-deb/download_file?file_path=reaper_${latest}_amd64.deb -O reaper_${latest}_amd64.deb
    sudo chown ubuntu:ubuntu reaper_${latest}_amd64.deb
fi