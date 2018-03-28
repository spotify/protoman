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

package protoman

import (
	"time"

	"github.com/pkg/errors"
	"github.com/spotify/protoman/cli/registry"
	"google.golang.org/grpc"
)

// DefaultTimeout is the timeout for all RPCs calls to the registry.
// TODO: Make this configurable.
var DefaultTimeout = 5 * time.Second

//NewRegistryClient returns a new registry client.
func NewRegistryClient(serverAddr string) (registry.SchemaRegistryClient, error) {
	conn, err := grpc.Dial(serverAddr, grpc.WithInsecure(), grpc.WithTimeout(time.Second))
	if err != nil {
		return nil, errors.New("unable to connect to registry at " + serverAddr)
	}
	return registry.NewSchemaRegistryClient(conn), nil
}
