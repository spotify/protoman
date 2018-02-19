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
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"

	"github.com/emicklei/proto"
	"github.com/pkg/errors"
	"github.com/spotify/protoman/cli/path"
	"github.com/spotify/protoman/cli/registry"
)

// getPackageName reads the package name from a .proto file
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

// checkPackageName validates that the proto package name matches the proto definition file's path
func checkPackageName(packageName string, path string) error {
	if packageName == "" {
		return fmt.Errorf("Missing package name")
	}
	dir := filepath.Dir(path)
	packagePath := strings.Replace(packageName, ".", "/", -1)
	if !strings.HasSuffix(dir, packagePath) {
		return fmt.Errorf("Package name does not match path, expected path to end with %s", packagePath)
	}
	return nil
}

// ValidateProto validates a .proto file and returns a ProtoFile
func ValidateProto(path string) (*registry.ProtoFile, error) {
	fmt.Printf("  Validating %s \n", path)

	f, err := os.Open(path)
	if err != nil {
		return nil, errors.Wrap(err, "Failed to open "+path)
	}
	defer f.Close()

	packageName, err := getPackageName(f)
	if err != nil {
		return nil, errors.Wrap(err, "Unable to parse proto file")
	}

	err = checkPackageName(packageName, path)
	if err != nil {
		return nil, err
	}

	f.Seek(0, 0)
	content, err := ioutil.ReadAll(f)
	if err != nil {
		return nil, errors.Wrap(err, "Unable to read proto file")
	}

	proto := &registry.ProtoFile{Path: packageName, Content: content}
	return proto, nil
}

// ValidateProtos valides .proto files and returns list of ProtoFiles
func ValidateProtos(protoPaths []string) ([]*registry.ProtoFile, error) {
	protos := make([]*registry.ProtoFile, len(protoPaths))
	for i, protoPath := range protoPaths {
		proto, err := ValidateProto(protoPath)
		if err != nil {
			return nil, errors.Wrap(err, "validation failed")
		}
		protos[i] = proto
	}
	return protos, nil
}

// Validate validates all .proto file under given directory
func Validate(root string) error {
	protoPaths, err := path.FindProtoFiles(root)
	if err != nil {
		return err
	}
	_, validationErr := ValidateProtos(protoPaths)
	return validationErr
}
