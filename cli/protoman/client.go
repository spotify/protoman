package protoman

import (
	"time"

	"github.com/pkg/errors"
	"github.com/spotify/protoman/cli/registry"
	"google.golang.org/grpc"
)

//NewRegistryClient returns a new registry client.
func NewRegistryClient(serverAddr string) (registry.SchemaRegistryClient, error) {
	conn, err := grpc.Dial(serverAddr, grpc.WithInsecure(), grpc.WithTimeout(time.Second))
	if err != nil {
		return nil, errors.New("unable to connect to registry at " + serverAddr)
	}
	return registry.NewSchemaRegistryClient(conn), nil
}
