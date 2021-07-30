#!/bin/bash

# Run this with
#
#   repo forall -v -c bash <full-path-to>/aggregator/remove-security-md.sh
#
# to remove the SECURITY.md file from all repositories
# that "repo" knows about.
#
# see https://issues.apache.org/jira/browse/SLING-10676 for more info
export ME=$(basename $0)

SEC=SECURITY.md
echo "Removing $SEC from $(basename $PWD)"

if [[ -f $ME ]]
then
    echo "Ignoring aggregator folder"
    exit 0
fi

if [[ ! -f $SEC ]]
then
    echo "$SEC not found, nothing to do"
    exit 0
fi

git checkout master
git pull origin master
git rm $SEC
git commit -m "SLING-10676 - remove $SEC which is not needed"
git push origin master

TOWAIT=5
echo "Waiting $TOWAIT seconds to reduce the git push load"
sleep $TOWAIT