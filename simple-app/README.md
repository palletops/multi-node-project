# simple-app

A simple app to deployed on a node.

The app is first deployed to a maven repository using `lein deploy
repository-name`.  The repository specified on the command line can be
configured in leiningen's configuration files.

From the maven repository, the deployer can pick up the files and deploy them to
the target environment.

## License

Copyright © 2013 Hugo Duncan

Distributed under the Eclipse Public License.
