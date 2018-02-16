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

package validator

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"

	"github.com/emicklei/proto"
	"github.com/pkg/errors"
	"github.com/spotify/protoman/cli/path"
	"github.com/spotify/protoman/cli/registry"
)

// getPackageName from proto
func getPackageName(r io.Reader) (string, error) {
	parser := proto.NewParser(r)
	def, err := parser.Parse()
	if err != nil {
		return "", err
	}
	for _, element := range def.Elements {
		switch element := element.(type) {
		case *proto.Package:
			return element.Name, nil
		}
	}
	return "", fmt.Errorf("Package name not found")
}

// ValidateProto validates a .proto file
func ValidateProto(filePath string) error {
	fmt.Printf("  Validating %s \n", filePath)
	f, err := os.Open(filePath)
	if err != nil {
		return errors.Wrap(err, "Failed to open "+filePath)
	}
	defer f.Close()
	packageName, err := getPackageName(f)
	if err != nil {
		return errors.Wrap(err, "Unable to parse proto file")
	}
	dir := filepath.Dir(filePath)
	packagePath := strings.Replace(packageName, ".", "/", -1)
	if !strings.HasSuffix(dir, packagePath) {
		return fmt.Errorf("Package path does not match filepath, expected directory path to end with " + packagePath)
	}
	return nil
}

func ValidateProtos(protos []*registry.ProtoFile) error {
	for _, proto := range protos {
		if err := ValidateProto(proto.Path); err != nil {
			return errors.Wrap(err, "invalid proto")
		}
	}
	return nil
}

// Validate validates all .proto file under given directory
func Validate(root string) error {
	protos, err := path.FindProtoFiles(root)
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
	return ValidateProtos(protos)
}
