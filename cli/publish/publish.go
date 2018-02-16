package publish

import (
	"context"
	"errors"
	"fmt"
	"os"
	"path/filepath"

	"github.com/spotify/protoman/cli/registry"
	"github.com/spotify/protoman/cli/validator"
	"google.golang.org/grpc"
)

// GetProtos in directory
func GetProtos(rootPath string) []string {
	list := []string{}
	err := filepath.Walk(rootPath, func(path string, info os.FileInfo, err error) error {
		if info.IsDir() {
			return nil
		}
		if filepath.Ext(path) == ".proto" {
			list = append(list, path)
		}
		return nil
	})
	if err != nil {
		fmt.Printf("walk error [%v]\n", err)
	}
	return list
}

func upload(protos []string, serverAddr string) error {
	fmt.Printf("Uploading to %s\n", serverAddr)
	conn, err := grpc.Dial(serverAddr)
	if err != nil {
		return errors.New("Unable to connect to registry at " + serverAddr)
	}
	defer conn.Close()
	client := registry.NewSchemaRegistryClient(conn)
	protoFiles := make([]*registry.ProtoFile, len(protos))
	for i := range protos {
		protoFiles[i].Path = protos[i]
	}
	request := registry.PublishSchemaRequest{ProtoFile: protoFiles}
	client.PublishSchema(context.Background(), &request)
	return nil
}

// Publish protobufs
func Publish(rootPath string) error {
	protos := GetProtos(rootPath)
	if len(protos) > 0 {
		fmt.Printf("Found %v proto schema(s)\n", len(protos))

		for _, proto := range protos {
			fmt.Printf("  Validating %s \n", proto)
			err := validator.ValidateProto(proto)
			if err != nil {
				fmt.Printf("%s is invalid: %v\n", proto, err)
				continue
			}
		}

		serverAddr := "localhost:9111"
		return upload(protos, serverAddr)
	}
	return nil
}
