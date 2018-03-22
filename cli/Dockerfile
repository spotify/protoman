FROM golang:alpine as app
RUN apk --no-cache add ca-certificates git
RUN mkdir -p /go/src/github.com/spotify/protoman/cli
WORKDIR /go/src/github.com/spotify/protoman/cli
COPY . .
RUN go get -u github.com/golang/dep/cmd/dep && dep ensure
RUN go build

FROM alpine:latest
RUN apk --no-cache add ca-certificates
WORKDIR /workdir/
COPY --from=app /go/src/github.com/spotify/protoman/cli/cli /bin/protoman
ENTRYPOINT [ "/bin/protoman" ]