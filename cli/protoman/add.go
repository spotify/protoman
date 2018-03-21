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
	"strings"
)

// Add protoman schema to config file
func Add(packageName, path string) error {
	if !strings.HasSuffix(path, strings.Replace(packageName, ".", "/", -1)) {
		return fmt.Errorf("package name does not match path, expected path to end with %s", path)
	}
	return addLocalPackage(ProtoPackage{Pkg: packageName, Path: path})
}
