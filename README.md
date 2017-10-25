# Apache Sling Aggregator

This module is part of the [Apache Sling](https://sling.apache.org) project.

## Retrieving all Sling modules

This module allows quick checkout of all Sling modules from Git. It requires
the local installation of the [repo](https://android.googlesource.com/tools/repo) tool.

```
$ curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
$ chmod a+x ~/bin/repo
```

See the detailed instructions at https://source.android.com/source/downloading#installing-repo.

```
$ repo init --no-clone-bundle https://github.com/apache/sling-aggregator.git
$ repo sync --no-clone-bundle
```

The output is a flat list of all Sling modules.
