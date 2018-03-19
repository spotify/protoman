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
	"time"

	"github.com/pkg/errors"
	"github.com/spotify/protoman/cli/path"
	"github.com/spotify/protoman/cli/registry"
	"github.com/spotify/protoman/cli/validator"
)

func formatResponse(err *registry.Error, violations []*registry.ValidationViolation) error {
	if violations != nil {
		fmt.Println("Violations:")
		for _, v := range violations {
			fmt.Printf("Description: %s\nCurrent: %s\nCandidate: %s\n", v.Description, v.Current, v.Candidate)
		}
	}
	if err != nil {
		return fmt.Errorf("Error: %s", err.Message)
	}
	return nil
}

func upload(protoFiles []*registry.ProtoFile, client registry.SchemaRegistryClient) error {
	request := registry.PublishSchemaRequest{ProtoFile: protoFiles}
	ctx, _ := context.WithTimeout(context.Background(), time.Second*3)

	resp, err := client.PublishSchema(ctx, &request)
	if err != nil {
		return errors.Wrap(err, "failed to upload schema")
	}
	return formatResponse(resp.Error, resp.Violation)
}

/*
Publish list of protoPaths to registry, will resolve local packages
defined .protoman configuration unless files are supplied.
*/
func Publish(protoPaths []string, client registry.SchemaRegistryClient) error {
	if len(protoPaths) == 0 {
		// No protos provided on command line, will upload local packages defined in .protoman
		c, err := readConfig()
		if err != nil {
			return err
		}

		for _, pkg := range c.Local {
			p, err := path.FindProtoFiles(pkg.Path)
			if err != nil {
				return err
			}
			protoPaths = append(protoPaths, p...)
		}
	}

	protos, validationErr := validator.ValidateProtos(protoPaths)
	if validationErr != nil {
		return validationErr
	}
	if len(protos) > 0 {
		return upload(protos, client)
	}
	return nil
}
