[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-aggregator&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-aggregator) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Aggregator

This module is part of the [Apache Sling](https://sling.apache.org) project.

It provides an [XML file](./default.xml) that lists all Sling modules, to allow for tools like `repo` to process multiple repositories at once, along
with some utility scripts to help manage our large number of repositories.

To update that XML file to include new Sling modules, run the [collect-sling-repos.groovy](./collect-sling-repos.groovy)
(with `-a`, see comments in the script), check the diffs of the [default.xml](./default.xml)
file and push the result if ok.

The list of modules is in a self-explaining format and can also be used in your own scripts if preferred.

The [list of repositories](https://sling.apache.org/repolist.html) on our website is also generated from this data using a
[Groovy page template](https://github.com/apache/sling-site/blob/master/src/main/jbake/templates/repolist.tpl).

Note that there are related efforts at [SLING-7331](https://issues.apache.org/jira/browse/SLING-7331) and [SLING-7262](https://issues.apache.org/jira/browse/SLING-7262), we'll need to consolidate all this at some point.

## Modules

You can find a list of the Apache Sling modules [here](docs/modules.md). 
This list is generated from the script [generate-aggregator-table.groovy](https://github.com/apache/sling-aggregator/blob/master/generate-aggregator-table.groovy).

### Updating Module Badges

We have a simple script to update the badges in GitHub's README.md files. To update a single repository:

    ./generate-project-badges.groovy [REPO_DIR]
    
To update all repositories:

    repo forall -c '[SLING_DIR]/aggregator/generate-project-badges.groovy .'

### Updating the .asf.yaml files

We maintain a set of [.asf.yaml](https://cwiki.apache.org/confluence/display/INFRA/Git+-+.asf.yaml+features) files, to set reasonable defaults for all Sling GitHub repositories. To make maintenance simple, we use the [scripts/update-asf-yaml.groovy](scripts/update-asf-yaml.groovy).

The script accepts a list of directories to generate or update the listing file for.

```bash
$ repo list --path-only | xargs groovy aggregator/scripts/update-asf-yaml.groovy
$ repo forall -c 'git add .asf.yaml'
$ repo forall -c 'git commit -m "SLING-12345 Updating .asf.yaml" -m "Detailed description"'
```

Once you are happy with what the commits look like you can push the changes, with a delay. Note that variations of `repo forall -c "sleep 5; git push"` don't seem to work, more research is required.

### Updating the Aggregator List

To update the aggregator list:

    groovy generate-aggregator-table.groovy [SLING_DIR]

### Prerequisites

 1. Use the repo tool to extract all of the repositories in the [sling aggregator](https://github.com/apache/sling-aggregator)
 2. Ensure you have SSH based access enabled to GitHub
 3. Ensure all repository workspaces are in a clean state

## Retrieving all Sling modules

This module allows quick checkout of all Sling modules from Git. It requires
the local installation of the [repo](https://android.googlesource.com/tools/repo) tool.

### Repo Tool Installation (all platforms)

```
$ curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
$ chmod a+x ~/bin/repo
```

See also the detailed instructions at https://source.android.com/source/downloading#installing-repo.

### Repo Tool Installation on Mac with [Homebrew](https://brew.sh)

    brew install repo

### Synchronizing all Git repositories

Initialise the local repo checkout and synchronise all git repositories. The commands below must be run in the sling-aggreator git checkout.

```
$ repo init --no-clone-bundle -u https://github.com/apache/sling-aggregator.git
$ repo sync --no-clone-bundle -j 16
$ repo forall -c 'git checkout master'
```

The last command ensures that all repositories are set to use the master branch. For some reason `repo` checks out
the latest revision but does not check out a specific branch.

The output is a flat list of all Sling modules.

The `-j 16` flag instructs repo to run 16 parallel checkout jobs and is added for performance reasons only.

#### Eclipse EGit Support

When trying to add such a repository with EGit in the local working copy is not detected due to the way that `repo` works with symbolic links. This has been reported in [Eclipse Bugzilla Issue #579042](https://bugs.eclipse.org/bugs/show_bug.cgi?id=579042).

### Be careful with "mass pushes" to repositories
ASF Infra notes that pushing to many Git repositories quickly can cause heavy load on their github
push queue. It's good to coordinate with them when doing such things, and/or add a few seconds of delay
between pushes. A delay of 5 seconds between each push worked well for [SLING-10676](https://issues.apache.org/jira/browse/SLING-10676)
when removing the SECURITY.md files that had been added unnecessarily, while adding them had raised alarms
due to fast pushes without delays.

### Updating the list of modules

That list is found in the [default.xml](./default.xml) file.

It is used to generate the [list of Git Repositories](http://sling.apache.org/repolist.html) on our website.

**Install Groovy with with [ASDF](https://asdf-vm.com/)**

    asdf install

**Install Groovy on Mac with [Homebrew](https://brew.sh)**

    brew install groovy

To update it:

    groovy collect-sling-repos.groovy > default.xml

Check changes with `git diff` and commit if needed.
