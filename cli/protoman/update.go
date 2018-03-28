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
	"github.com/spotify/protoman/cli/registry"
)

// Update all third party packages
func Update(client registry.SchemaRegistryClient) error {
	c, err := readConfig()
	if err != nil {
		return err
	}
	for _, pp := range c.ThirdParty {
		if err := getPackage(client, pp); err != nil {
			return err
		}
	}
	return nil
}
