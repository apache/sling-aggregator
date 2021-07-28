#!/bin/bash

# Run this with
#
#   repo forall -v -c bash <full-path-to>/aggregator/add-security-md.sh
#
# to add or update the SECURITY.md file from the
# "aggregator" project to all repositories found
# under it.
export ME=$(basename $0)

SEC=SECURITY.md
echo "Adding $SEC to $(basename $PWD)"

if [[ -f $ME ]]
then
    echo "Ignoring aggregator folder"
    exit 0
fi

git checkout master
git pull origin master
cat ../aggregator/$SEC > $SEC
git add $SEC
git commit -m "SLING-10676 - add or update $SEC"
# TODO git push origin master