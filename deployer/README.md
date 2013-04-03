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

For testing on virtualbox, there is a single node target.

```
lein pallet up --phases install,configure,start
```

## License

Copyright © 2013 Hugo Duncan

Distributed under the Eclipse Public License.
