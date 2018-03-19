# Protoman CLI

# Usage

Generate new proto

```bash
protoman generate $PACKAGE $SERVICE_NAME $PROTO_ROOT
protoman generate spotify.foobar winning src/main/proto
```

Get dependency

```bash
protoman get -s $SERVER -p $PROTO_ROOT $PACKAGES
```

Publish local packages

```bash
protoman publish -s $SERVER
```

.protoman.yaml keep track of local and third party dependencies.

Example content:

```yaml
local: []
third_party:
- path: src/main/protos/spotify/awesome
  pkg: jhaals.awesome
- path: protos/src/main/winning/jhaals/testing
  pkg: jhaals.testing
```

### Building

To compile protoman CLI, run:

```sh
make compile
```

To just test it, run:

```sh
make test
```