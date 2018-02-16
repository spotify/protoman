package publish

import (
	"context"
	"errors"
	"fmt"
	"os"

	"github.com/spotify/protoman/cli/path"
	"github.com/spotify/protoman/cli/registry"
	"github.com/spotify/protoman/cli/validator"
	"google.golang.org/grpc"
)

func upload(protoFiles []*registry.ProtoFile, serverAddr string) error {
	fmt.Printf("Uploading to %s\n", serverAddr)
	conn, err := grpc.Dial(serverAddr)
	if err != nil {
		return errors.New("unable to connect to registry at " + serverAddr)
	}
	defer conn.Close()
	client := registry.NewSchemaRegistryClient(conn)
	request := registry.PublishSchemaRequest{ProtoFile: protoFiles}
	client.PublishSchema(context.Background(), &request)
	return nil
}

// Publish publishes all .proto file under given directory
func Publish(root string) error {
	protos, err := path.FindProtoFiles(root)
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	if err := validator.ValidateProtos(protos); err != nil {
		return err
	}
	if len(protos) > 0 {
		serverAddr := "localhost:9111"
		return upload(protos, serverAddr)
	}
	return nil
}
