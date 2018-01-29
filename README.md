# Apache Sling Aggregator

This module is part of the [Apache Sling](https://sling.apache.org) project.

It provides an XML file that lists all Sling modules, to allow for tools like `repo` to process multiple repositories at once.

The list of modules is in a self-explaining format and can also be used in your own scripts if preferred.

Note that there are related efforts at [SLING-7331](https://issues.apache.org/jira/browse/SLING-7331) and [SLING-7262](https://issues.apache.org/jira/browse/SLING-7262), we'll need to consolidate all this at some point.

## Retrieving all Sling modules

This module allows quick checkout of all Sling modules from Git. It requires
the local installation of the [repo](https://android.googlesource.com/tools/repo) tool.

```
$ curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
$ chmod a+x ~/bin/repo
```

**Install Repo on Mac with [Homebrew](https://brew.sh)**

    brew install repo

See the detailed instructions at https://source.android.com/source/downloading#installing-repo.

```
$ repo init --no-clone-bundle -u https://github.com/apache/sling-aggregator.git
$ repo sync --no-clone-bundle
```

The output is a flat list of all Sling modules.

### Speeding up sync

Syncing all Sling modules can take a while, so if your network is fast enough you can try using multiple jobs in parallel. To use 16 jobs, run

```
$ repo sync -j 16
```

### Updating the list of modules

That list is found in the [default.xml](./default.xml) file.

It is used to generate the [list of Git Repositories](http://sling.apache.org/repolist.html) on our website.

**Install Groovy on Mac with [Homebrew](https://brew.sh)**

    brew install groovy

To update it:

    groovy collect-sling-repos.groovy > default.xml

Check changes with `git diff` and commit if needed.
