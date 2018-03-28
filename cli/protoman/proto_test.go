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

import "testing"

func TestProtoPackage(t *testing.T) {
	passTests := []struct {
		pkg             string
		path            string
		expectedPkgPath string
		expectedAbsPath string
	}{
		{
			pkg:             "spotify.foobar.v1",
			path:            "src/main/proto",
			expectedAbsPath: "src/main/proto/spotify/foobar/v1",
			expectedPkgPath: "spotify/foobar/v1",
		},
		{
			pkg:             "spotify.foobar.v1",
			path:            "module/src/main/proto",
			expectedAbsPath: "module/src/main/proto/spotify/foobar/v1",
			expectedPkgPath: "spotify/foobar/v1",
		},
	}

	for _, test := range passTests {
		pp, err := newProtoPackage(test.path, test.pkg)
		if err != nil {
			t.Fatal(err)
		}
		if pp.pkgPath() != test.expectedPkgPath {
			t.Errorf("Expected pkg to be %v, got %v instead.", test.expectedPkgPath, pp.pkgPath())
		}
		if pp.absPath() != test.expectedAbsPath {
			t.Errorf("Expected absPath to be %v, got %v instead.", test.expectedAbsPath, pp.absPath())
		}
	}

	failTests := []struct {
		pkg string
	}{
		{
			pkg: "spotify/foobar/v1",
		},
		{
			pkg: ".spotify.foobar.v1",
		},
		{
			pkg: "spotify.foobar.v1.",
		},
		{
			pkg: ".",
		},
	}

	for _, test := range failTests {
		_, err := newProtoPackage("src/main/proto", test.pkg)
		if err == nil {
			t.Fatalf("Package with name %v is expected to fail.", test.pkg)
		}
	}
}
