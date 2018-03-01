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
	"path/filepath"

	"github.com/spf13/viper"
)

// config struct
type config struct {
	configPath string
}

func (c *config) Read() error {
	viper.AddConfigPath(c.configPath)
	viper.SetConfigType("yaml")
	viper.SetConfigName(".protoman")
	return viper.ReadInConfig()
}

// Init new protoman configuration file
func (c *config) Init(packagePath, rootPath string) error {
	viper.Set("local", []string{packagePath})
	viper.Set("root-path", rootPath)
	return viper.WriteConfigAs(filepath.Join(c.configPath, ".protoman.yaml"))
}

// AddPackage to .protoman.yaml
func (c *config) AddPackage(path, packageType string) error {
	dependencies := viper.GetStringSlice(packageType)
	if dependencies == nil {
		viper.Set(packageType, []string{path})
	} else {
		for _, pkg := range dependencies {
			if pkg == path {
				// package already added
				return nil
			}
		}
		viper.Set(packageType, append(dependencies, path))
	}
	return viper.WriteConfigAs(filepath.Join(c.configPath, ".protoman.yaml"))
}

// GetPackages from configuration
func (c *config) GetPackages(packageType string) []string {
	return viper.GetStringSlice(packageType)
}
