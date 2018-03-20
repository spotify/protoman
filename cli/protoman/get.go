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
	for _, p := range packages {
		if err := getPackage(client, p); err != nil {
			return err
		}
		if err := addThirdPartyPackage(p); err != nil {
			return err
		}
	}
	return nil
}

func getPackage(client registry.SchemaRegistryClient, p ProtoPackage) error {
	request := registry.GetSchemaRequest{
		Request: []*registry.GetSchemaRequest_RequestedPackage{
			&registry.GetSchemaRequest_RequestedPackage{Package: p.Pkg},
		},
	}
	// Get schema(s) from registry.
	ctx, _ := context.WithTimeout(context.Background(), time.Second*3)
	resp, err := client.GetSchema(ctx, &request)
	if err != nil {
		return errors.Wrap(err, "failed to get schema(s) from registry")
	}

	// Store each proto in package path
	for _, protoFile := range resp.ProtoFile {
		if err := os.MkdirAll(filepath.Join(p.Path, filepath.Dir(protoFile.Path)), 0755); err != nil {
			return errors.Wrap(err, "failed to create package path")
		}
		path := filepath.Join(
			filepath.Join(p.Path, filepath.Dir(protoFile.Path)),
			filepath.Base(protoFile.Path))
		err := ioutil.WriteFile(path,
			[]byte(protoFile.Content), 0644)
		if err != nil {
			return errors.Wrap(err, "failed to write protofile")
		}
	}
	return nil
}
