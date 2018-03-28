package main

import (
  "flag"
  "net/http"

  "github.com/golang/glog"
  "golang.org/x/net/context"
  "github.com/grpc-ecosystem/grpc-gateway/runtime"
  "google.golang.org/grpc"

  gw "github.com/spotify/protoman/http-proxy/registry"
)

var (
  echoEndpoint = flag.String("echo_endpoint", "localhost:8080", "endpoint of YourService")
)

func run() error {
  ctx := context.Background()
  ctx, cancel := context.WithCancel(ctx)
  defer cancel()

  mux := runtime.NewServeMux()
  opts := []grpc.DialOption{grpc.WithInsecure()}
  err := gw.RegisterSchemaProtodocHandlerFromEndpoint(ctx, mux, *echoEndpoint, opts)
  if err != nil {
    return err
  }

  return http.ListenAndServe(":9090", mux)
}

func main() {
  flag.Parse()
  defer glog.Flush()

  if err := run(); err != nil {
    glog.Fatal(err)
  }
}