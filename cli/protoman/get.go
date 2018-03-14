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
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/spotify/protoman/cli/registry"
	"google.golang.org/grpc"
)

// Get package from protoman
func Get(packages []string, path, serverAddr string) error {
	// No packages provided, try to find them in .protoman configuration file
	if len(packages) == 0 {
		c, err := readConfig()
		if err != nil {
			return err
		}

		for _, pkg := range c.ThirdParty {
			packages = append(packages, pkg)
		}
	}

	if len(packages) == 0 {
		return fmt.Errorf("No packages provided on command line or in .protoman configuration")
	}

	request := registry.GetSchemaRequest{
		Request: []*registry.GetSchemaRequest_RequestedPackage{},
	}
	// Create package directories and append package name to RPC request.
	for _, packageName := range packages {
		request.Request = append(request.Request, &registry.GetSchemaRequest_RequestedPackage{Package: packageName})
		pkgPath := filepath.Join(path, strings.Replace(packageName, ".", "/", -1))
		if err := os.MkdirAll(pkgPath, 0755); err != nil {
			return errors.Wrap(err, "failed to create package path")
		}
	}
	conn, err := grpc.Dial(serverAddr, grpc.WithInsecure(), grpc.WithTimeout(time.Second))
	if err != nil {
		return errors.New("unable to connect to registry at " + serverAddr)
	}
	defer conn.Close()
	ctx, _ := context.WithTimeout(context.Background(), time.Second*3)
	client := registry.NewSchemaRegistryClient(conn)
	resp, err := client.GetSchema(ctx, &request)
	if err != nil {
		return errors.Wrap(err, "failed to get schema(s) from registry")
	}

	// Store each proto in proto path.
	for _, protoFile := range resp.ProtoFile {
		err := ioutil.WriteFile(
			filepath.Join(path, protoFile.Path),
			[]byte(protoFile.Content), 0644)
		if err != nil {
			return errors.Wrap(err, "failed to write protofile")
		}
	}
	// Add packages to .protoman
	for _, packageName := range packages {
		if err := addThirdPartyPackage(packageName); err != nil {
			return err
		}
	}
	return nil
}
