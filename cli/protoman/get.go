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
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"

	"github.com/pkg/errors"
	"github.com/spotify/protoman/cli/registry"
)

// Get package from protoman
func Get(client registry.SchemaRegistryClient, path string, pkgs []string) error {
	fi, err := os.Stat(path)
	if os.IsNotExist(err) {
		return err
	}
	if err != nil {
		return err
	}
	if !fi.IsDir() {
		return fmt.Errorf("%s is not a directory", path)
	}

	for _, pkg := range pkgs {
		pp, err := newProtoPackage(path, pkg)
		if err != nil {
			return err
		}

		if err := getPackage(client, pp); err != nil {
			return err
		}

		if err := addThirdPartyPackage(pp); err != nil {
			return err
		}
	}
	return nil
}

var errSchemaNotFound = errors.New("schema not found")

func getPackage(client registry.SchemaRegistryClient, pp *protoPackage) error {
	// Get schema(s) from registry.
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()

	resp, err := client.GetSchema(ctx, &registry.GetSchemaRequest{
		Request: []*registry.GetSchemaRequest_RequestedPackage{
			&registry.GetSchemaRequest_RequestedPackage{
				Package: pp.Pkg,
			},
		},
	})
	if err != nil {
		return errors.Wrap(err, "failed to get schema(s) from registry")
	}
	if len(resp.ProtoFile) == 0 {
		return errSchemaNotFound
	}

	// Store each proto in package path
	for _, protoFile := range resp.ProtoFile {
		if err := os.MkdirAll(filepath.Join(pp.Path, filepath.Dir(protoFile.Path)), 0755); err != nil {
			return errors.Wrap(err, "failed to create package path")
		}
		path := filepath.Join(
			filepath.Join(pp.Path, filepath.Dir(protoFile.Path)),
			filepath.Base(protoFile.Path))
		err := ioutil.WriteFile(path,
			[]byte(protoFile.Content), 0644)
		if err != nil {
			return errors.Wrap(err, "failed to write protofile")
		}
	}

	return nil
}
