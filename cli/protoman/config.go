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

	"github.com/pkg/errors"
	yaml "gopkg.in/yaml.v2"
)

// config
type config struct {
	Local      []string `yaml:"local"`
	ThirdParty []string `yaml:"third_party"`
}

func (c *config) read() (*config, error) {
	if _, err := os.Stat(".protoman"); os.IsNotExist(err) {
		_, err := os.OpenFile(".protoman", os.O_RDONLY|os.O_CREATE, 0644)
		if err != nil {
			return c, errors.Wrap(err, "Failed to create .protoman config file")
		}
	}
	data, err := ioutil.ReadFile(".protoman")
	if err != nil {
		return c, err
	}
	err = yaml.Unmarshal([]byte(data), c)
	if err != nil {
		return c, err
	}
	return c, err
}

func (c *config) write(cfg *config) error {
	data, err := yaml.Marshal(cfg)
	if err != nil {
		return err
	}
	return ioutil.WriteFile(".protoman", data, 0644)
}

// AddLocalPackage to .protoman
func (c *config) AddLocalPackage(path string) error {
	cfg, err := c.read()
	if err != nil {
		return nil
	}
	for _, pkg := range cfg.Local {
		if pkg == path {
			return nil
		}
	}
	cfg.Local = append(cfg.Local, path)
	return c.write(cfg)
}

// AddLocalPackage to .protoman
func (c *config) AddThirdPartyPackage(path string) error {
	cfg, err := c.read()
	if err != nil {
		return nil
	}
	for _, pkg := range cfg.ThirdParty {
		if pkg == path {
			return nil
		}
	}
	cfg.ThirdParty = append(cfg.ThirdParty, path)
	return c.write(cfg)
}
