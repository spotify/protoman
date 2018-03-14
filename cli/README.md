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
protoman publish -s $SERVER -p $PROTO_ROOT
```

.protoman.yaml keep track of local and third party dependencies.

Example content:

```yaml
third_party:
- spotify.apa
- spotify.win
local:
- spotify.foobar
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