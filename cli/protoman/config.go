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
	"io/ioutil"
	"os"

	yaml "gopkg.in/yaml.v2"
)

type config struct {
	Local      []string `yaml:"local"`
	ThirdParty []string `yaml:"third_party"`
}

const defaultConfig = ".protoman"

// addLocalPackage to config.
func addLocalPackage(path string) error {
	cfg, err := readConfig()
	if err != nil {
		return nil
	}

	for _, pkg := range cfg.Local {
		if pkg == path {
			return nil
		}
	}

	cfg.Local = append(cfg.Local, path)
	return writeConfig(cfg)
}

// addThirdPartyPackage to config.
func addThirdPartyPackage(path string) error {
	cfg, err := readConfig()
	if err != nil {
		return nil
	}

	for _, pkg := range cfg.ThirdParty {
		if pkg == path {
			return nil
		}
	}

	cfg.ThirdParty = append(cfg.ThirdParty, path)
	return writeConfig(cfg)
}

func createOnNotExist(err error) error {
	if os.IsNotExist(err) {
		f, err := os.OpenFile(defaultConfig, os.O_RDONLY|os.O_CREATE, 0644)
		if err != nil {
			return err
		}
		defer f.Close()
		return nil
	}
	return err
}

func readConfig() (*config, error) {
	var c config

	data, err := ioutil.ReadFile(defaultConfig)
	if err = createOnNotExist(err); err != nil {
		return &c, err
	}

	err = yaml.Unmarshal([]byte(data), &c)
	if err != nil {
		return &c, err
	}

	return &c, err
}

func writeConfig(cfg *config) error {
	data, err := yaml.Marshal(cfg)
	if err != nil {
		return err
	}

	return ioutil.WriteFile(defaultConfig, data, 0644)
}
