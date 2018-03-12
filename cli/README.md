# Protoman CLI

# Usage

Generate new proto

```bash
protoman generate $PACKAGE $SERVICE_NAME $PROTO_ROOT
protoman generate spotify.foobar winning src/main/proto
```

Get dependency

```bash
protoman get $PACKAGE $PROTO_ROOT
protoman get spotify.random src/main/proto
```

.protoman.yaml keep track of local and third party dependencies.

Example content:

```yaml
third_party:
- src/main/proto/spotify/apa
- src/main/proto/spotify/win
local:
- src/main/proto/spotify/foobar
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