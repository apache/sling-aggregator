#!/bin/bash -eu

if [ ! -f pom.xml ]; then
    echo "No pom.xml, skipping"
    exit 0
fi

origin=$(git remote get-url origin)
sed -i "s#<url>https://gitbox.apache.org/repos/asf.*</url>#<url>${origin}</url>#" pom.xml
