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
	"context"
	"io/ioutil"
	"os"
	"path/filepath"
	"time"

	"github.com/pkg/errors"
	"github.com/spotify/protoman/cli/registry"
)

// Get package from protoman
func Get(packages []ProtoPackage, rootPath string, client registry.SchemaRegistryClient) error {
	request := registry.GetSchemaRequest{
		Request: []*registry.GetSchemaRequest_RequestedPackage{},
	}
	// Create package directories and append package name to RPC request.
	for _, p := range packages {
		request.Request = append(request.Request, &registry.GetSchemaRequest_RequestedPackage{Package: p.Pkg})
		if err := os.MkdirAll(p.Path, 0755); err != nil {
			return errors.Wrap(err, "failed to create package path")
		}
	}

	// Get schema(s) from registry.
	ctx, _ := context.WithTimeout(context.Background(), time.Second*3)
	resp, err := client.GetSchema(ctx, &request)
	if err != nil {
		return errors.Wrap(err, "failed to get schema(s) from registry")
	}

	// Store each proto in proto path.
	for _, protoFile := range resp.ProtoFile {
		err := ioutil.WriteFile(
			filepath.Join(rootPath, protoFile.Path),
			[]byte(protoFile.Content), 0644)
		if err != nil {
			return errors.Wrap(err, "failed to write protofile")
		}
	}

	return addThirdPartyPackages(packages...)
}
