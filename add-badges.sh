#!/usr/bin/env bash
trap ctrl_c INT

GRIP_PID=-1

function ctrl_c() {
    stop_grip
    exit 0
}

function stop_grip() {
    if [[ (-n "$GRIP_PID") && ("$GRIP_PID" != "-1") ]]; then
        echo "Stoping grip..."
        kill -9 $GRIP_PID 2>&1 > /dev/null
        echo "Done."
    fi
}

SCRIPT_DIR=$PWD

AUTO_COMMIT=0

function prepend () {
    echo -e "$LINE$(cat README.md)" > README.md
}

function overwrite_readme () {
    PROJECT_NAME="$(xpath pom.xml '/project/name/text()' | xargs)" > /dev/null 2>&1
    PROJECT_DESCRIPTION="$(xpath pom.xml '/project/description/text()' | xargs)" > /dev/null 2>&1
    
    echo "Overwriting README for $PROJECT_NAME"
    
    printf "# $PROJECT_NAME\n\nThis module is part of the [Apache Sling](https://sling.apache.org) project.\n\n$PROJECT_DESCRIPTION" > README.md
    update_badges
}

function update_badges () {
    echo "Updating badges on $REPO"
    REPO_NAME=${PWD##*/}
    ARTIFACT_ID="$(xpath pom.xml '/project/artifactId/text()')" > /dev/null 2>&1
    echo "Artifact ID: $ARTIFACT_ID"
    
    GIT=$(git remote -v)
    if [[ "$GIT" = *"https"* ]]; then
        git checkout master
        git remote remove origin
        git remote add origin git@github.com:apache/sling-$REPO_NAME.git
        git fetch
        git branch --set-upstream-to=origin/master master
    fi
    
    echo "Adding standard items for $REPO_NAME"
    LINE="\n\n"
    prepend
    
    STATUS=""
    for module in `cat $SCRIPT_DIR/contrib-projects.txt`; do
        if [[ "$module" == "$REPO_NAME" ]]; then
            STATUS="contrib"
            echo "Found status $STATUS..."
            break
        fi
    done

    if [[ -z $STATUS ]]; then
        for module in `cat $SCRIPT_DIR/deprecated-projects.txt`; do
            if [[ "$module" == "$REPO_NAME" ]]; then
                STATUS="deprecated"
                echo "Found status $STATUS..."
                break
            fi
        done
    fi
    
    if [ ! -z $STATUS ]; then
        LINE="&#32;[![$STATUS](https://sling.apache.org/badges/status-$STATUS.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/status/$STATUS.md)"
        prepend
    fi
    
    GROUP="$(xmllint --xpath "string(/manifest/project[@path=\"$REPO_NAME\"]/@groups)" $SCRIPT_DIR/default.xml)"
    if [ ! -z "$GROUP" ]; then
        echo "Found group $GROUP..."
        LINE=" [![${GROUP}](https://sling.apache.org/badges/group-$GROUP.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/$GROUP.md)"
        prepend
    fi
    
    LINE=" [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)"
    prepend
    
    if [[ ! -z $ARTIFACT_ID ]]; then
        JAVADOC_BADGE_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://www.javadoc.io/badge/org.apache.sling/$ARTIFACT_ID.svg)
        if [[ $JAVADOC_BADGE_RESPONSE != "404" ]]; then
            echo "Adding Javadoc badge for $ARTIFACT_ID"
            LINE=" [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/$ARTIFACT_ID.svg)](https://www.javadoc.io/doc/org.apache.sling/$ARTIFACT_ID)"
            prepend
        else
            echo "No published javadocs found for $ARTIFACT_ID"
        fi
    
        MAVEN_BADGE_CONTENTS=$(curl -L https://maven-badges.herokuapp.com/maven-central/org.apache.sling/$ARTIFACT_ID/badge.svg)
        if [[ $MAVEN_BADGE_CONTENTS = *"unknown"* ]]; then
            echo "No Maven release found for $ARTIFACT_ID"
        else
            echo "Adding Maven release badge for $ARTIFACT_ID"
            LINE=" [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/$ARTIFACT_ID/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22$ARTIFACT_ID%22)"
            prepend
        fi
    fi
    
    STATUS_CONTENTS=$(curl -L "https://sonarcloud.io/api/project_badges/measure?project=apache_sling-$REPO_NAME&metric=alert_status")
    if [[ $STATUS_CONTENTS = *"Measure has not been found"* ]]; then
        echo "No sonarcloud status found for $REPO_NAME"
    else
        echo "Adding sonarcloud status badge for $REPO_NAME"
        LINE=" [![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-$REPO_NAME&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-$REPO_NAME)"
        prepend
    fi
    
    COVERAGE_CONTENTS=$(curl -L "https://sonarcloud.io/api/project_badges/measure?project=apache_sling-$REPO_NAME&metric=coverage")
    if [[ $COVERAGE_CONTENTS = *"Measure has not been found"* ]]; then
        echo "No coverage reports found for $REPO_NAME"
    else
        echo "Adding coverage badge for $REPO_NAME"
        LINE=" [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-$REPO_NAME&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-$REPO_NAME)"
        prepend
    fi
    
    TEST_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-$REPO_NAME/job/master/")
    if [ "$TEST_RESPONSE" = "404" ]; then
        echo "No tests found for $REPO_NAME"
    else
        echo "Adding test badge for $REPO_NAME"
        LINE=" [![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-$REPO_NAME/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-$REPO_NAME/job/master/test/?width=800&height=600)"
        prepend
    fi
    
    BUILD_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://ci-builds.apache.org/job/Sling/job/modules/job/sling-$REPO_NAME/job/master/badge/icon)
    if [ "$BUILD_RESPONSE" != "404" ]; then
        echo "Adding build badge for $REPO_NAME"
        LINE=" [![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-$REPO_NAME/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-$REPO_NAME/job/master/)"
        prepend
    else
        echo "No build found for $REPO_NAME"
    fi
    
    echo "Adding logo for $REPO_NAME"
    LINE="[<img src=\"https://sling.apache.org/res/logos/sling.png\"/>](https://sling.apache.org)\n\n"
    prepend
    

    GRIP_PID=$(pgrep -f grip)
    if [[ -n $GRIP_PID} ]]; then
        stop_grip 2>&1 > /dev/null
    fi
    grip -b > /dev/null 2>&1 & > /dev/null
    GRIP_PID=$(pgrep -f grip)
    if [[ ${AUTO_COMMIT} -eq 1 ]]; then
        RESULTS="C"
    else
        if [[ ! -z $ARTIFACT_ID ]]; then
            echo "Commit results? (C=Commit,N=No,R=Revert,O=Overwrite README)?"
        else
            echo "Commit results? (C=Commit,N=No,R=Revert)?"
        fi
        read RESULTS
    fi
    
    if [ "$RESULTS" == "C" ]; then
        git commit README.md -m "Updating badges for ${REPO_NAME}"
    elif [ "$RESULTS" == "R" ]; then
        git checkout -- README.md
    elif [ "$RESULTS" == "O" ]; then
        git checkout -- README.md
        overwrite_readme
    fi
    stop_grip
}

function handle_repo () {
    cd $REPO
    if [ ! -e "README.md" ]; then
        echo "No README.md found in $REPO"
    elif egrep -q "https?:\/\/sling\.apache\.org\/res\/logos\/sling\.png" "README.md"; then
        if [[ ${AUTO_COMMIT} -eq 1 ]]; then
            OVERWRITE="Y"
        else
            echo "Badge already present on $REPO, overwrite (Y/N)?"
            read OVERWRITE
        fi
        if [ "$OVERWRITE" == "Y" ]; then
            sed -i -e 1,4d README.md
            rm README.md-e
            update_badges
        else
            echo "Skipping..."
        fi
    else
        update_badges
    fi
}

while getopts "o" opt; do
    case "$opt" in
        o)
            AUTO_COMMIT=1
            shift
            ;;
    esac
done

# if [[ ${AUTO_COMMIT} -eq 1 ]]; then
#     SLING_DIR=$1
#     PROJECT=$2
# else
#
# fi

SLING_DIR=$1
PROJECT=$2


if [ ! -f ~/.grip/settings.py ]; then
    echo "Did not find GitHub Access token file, please generate an access token on GitHub https://github.com/settings/tokens/new?scopes= and provide it below:"
    read ACCESS_TOKEN
    echo "PASSWORD = '$ACCESS_TOKEN'" > ~/.grip/settings.py
fi

printf "\nStarting badge update!\n\n-------------------------\n\n"
if [ -z "$SLING_DIR" ]; then
    echo "Please provide the Sling Directory: ./add-badges.sh [SLING_DIR]"
    exit 1
fi

if [ -z "$PROJECT" ]; then
    echo "Handling all repos in $SLING_DIR"
    for REPO in $SLING_DIR/*/ ; do
        handle_repo
    done
else
    echo "Handling project $SLING_DIR/$PROJECT"
    REPO=$SLING_DIR/$PROJECT
    handle_repo
fi
printf "\n\nBadge Update Complete!"
