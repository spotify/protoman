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
	"path"
	"regexp"
	"strings"
)

type protoPackage struct {
	// Path is the root path where the protocol buffers are located on disk relative
	// to the .protoman file. Example src/main/proto.
	Path string
	// Pkg is the name of the protocol buffers package specifier. Example spotify.foobar.v1.
	Pkg string
}

func newProtoPackage(path, pkg string) (*protoPackage, error) {
	if !validPkgName(pkg) {
		return nil, fmt.Errorf("package name can only contain alphanumeric characters, numbers and dots")
	}

	return &protoPackage{
		Path: path,
		Pkg:  pkg,
	}, nil
}

// absPath returns the absolute path to the protocol buffers package.
// Example src/main/proto/spotify/foobar/v1
func (pp *protoPackage) absPath() string {
	return path.Join(pp.Path, pp.pkgPath())
}

// pkgPath returns the path to the protocol buffers package.
// Example spotify/foobar/v1
func (pp *protoPackage) pkgPath() string {
	return strings.Replace(pp.Pkg, ".", "/", -1)
}

// rePkgName matches alphanumeric characters, numbers and dots.
var rePkgName = regexp.MustCompile("^[a-zA-Z0-9.]+$")

func validPkgName(pkg string) bool {
	if strings.HasPrefix(pkg, ".") || strings.HasSuffix(pkg, ".") {
		return false
	}
	return rePkgName.MatchString(pkg)
}
