# deployer

An outline pallet project to enable the deployment of applications to multiple
nodes.

The outline project provides a minimal install of postgres and redis.

## Cluster Configurations

## Prerequisites

Install [leiningen](https://github.com/technomancy/leiningen#installation).

Install a ubuntu 12.04 vmfest image.

```
lein pallet add-vmfest-image https://s3.amazonaws.com/vmfest-images/ubuntu-12.04.vdi.gz
```

## Launching on Virtualbox

For testing on virtualbox, there is a single node target. To bring this up:

```
lein pallet up --phases install,configure,start --roles all-in-one
```

To remove the vm:

```
lein pallet down --roles all-in-one
```

For a two node setup, with the databases in one vm, and an application in the
other, the commands are:

```
lein pallet up --phases install,configure,start --roles dev
```

```
lein pallet down --roles dev
```

## License

Copyright © 2013 Hugo Duncan

Distributed under the Eclipse Public License.
