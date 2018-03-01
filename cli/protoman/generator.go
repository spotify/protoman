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
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/alecthomas/template"
	"github.com/pkg/errors"
)

const protoTemplate = `syntax = "proto3";
package {{ .PackageName }};

service {{ .ServiceName }} {
    rpc Ping (PingRequest) returns (PingResponse) {}
}
message PingRequest {
}
message PingResponse {
}
`

type templateParams struct {
	PackageName string
	ServiceName string
}

// Generate for language
func Generate(packageName, serviceName, rootPath string) error {
	if _, err := os.Stat(".protoman.yaml"); err == nil {
		return fmt.Errorf("Protoman is already initialized in this project.\n Delete the .protoman.yaml to clear previous configuration")
	}
	// Convert spotify.foobar to spotify/foobar
	packagePath := strings.Replace(packageName, ".", "/", -1)
	path := filepath.Join(rootPath, packagePath)
	cfg := config{configPath: "."}
	err := cfg.Init(packagePath, rootPath)
	if err != nil {
		return errors.Wrap(err, "Failed to initialize configuration")
	}
	err = os.MkdirAll(path, 0755)
	if err != nil {
		return errors.Wrap(err, "failed to create package path")
	}
	t := template.Must(template.New("proto").
		Parse(protoTemplate))

	f, err := os.Create(filepath.Join(path, fmt.Sprintf("%s.proto", serviceName)))
	if err != nil {
		return errors.Wrap(err, "failed to create proto file")
	}
	err = t.Execute(f, templateParams{ServiceName: serviceName, PackageName: packageName})
	if err != nil {
		return err
	}

	return nil
}
