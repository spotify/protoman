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

package path

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/pkg/errors"
)

// FindProtoFiles return list over all .proto files under given directory
func FindProtoFiles(rootPath string) ([]string, error) {
	protoFiles := []string{}
	if _, err := os.Stat(rootPath); err != nil {
		return nil, fmt.Errorf("%s does not exist", rootPath)
	}
	err := filepath.Walk(rootPath, func(path string, info os.FileInfo, err error) error {
		if info.IsDir() {
			return nil
		}
		if filepath.Ext(path) == ".proto" {
			protoFiles = append(protoFiles, path)
		}
		return nil
	})
	if err != nil {
		errors.Wrap(err, "error traversing directory "+rootPath)
	}
	fmt.Printf("Found %v proto schema file(s)\n", len(protoFiles))
	return protoFiles, nil
}
