/*
Copyright 2018 Spotify AB. All rights reserved.

The contents of this file are licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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
