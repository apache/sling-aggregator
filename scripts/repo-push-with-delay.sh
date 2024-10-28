#!/bin/bash -eu

repos=$(repo list | awk '{print $1}')
delay_seconds=5

# Loop through each repository
for repo in $repos; do
    pushd "$repo"
    git push || true
    echo "Pushed changes to $repo"
    sleep $delay_seconds
    popd
done
