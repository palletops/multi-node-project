# simple-app

A simple app to deployed on a node.

The app is first deployed to a maven repository using `lein deploy
repository-name`.  The repository specified on the command line can be
configured in leiningen's configuration files.

From the maven repository, the deployer can pick up the files and deploy them to
the target environment.

The leiningen `profiles.clj` file specifies a `test-repo` profile, with a
`test-repo` repository.  You can deploy to this using:

```
lein with-profile +test-repo deploy test-repo
```

You can use AWS S3 as a repository, by using the
[s3-wagon-private](https://github.com/technomancy/s3-wagon-private) plugin.

Credentials can be configured in `profiles.clj`, which supports pgp encryption
of passwords via the `credentials.clj.pgp` file.

## License

Copyright © 2013 313 Ventures LLC

Distributed under the Eclipse Public License.
