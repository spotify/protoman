# Protoman CLI

# Usage

Initialize new repository

```bash
protoman init $PACKAGE $SERVICE_NAME -p $PROTO_ROOT
protoman init spotify.foobar winning -p src/main/proto
```

Get dependency

```bash
protoman get $PACKAGE
protoman get spotify.random
```

.protoman.yaml keep track of local and third party dependencies.

Example content:

```yaml
3rd_party:
- spotify/apa
- spotify/win
local:
- spotify/foobar
root-path: src/main/proto
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