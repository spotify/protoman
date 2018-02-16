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
	"strings"
	"testing"
)

func TestGetPackage(t *testing.T) {
	tt := []struct {
		name                string
		shouldFail          bool
		input               string
		expectedPackageName string
	}{
		{
			name:       "Bad protobuf",
			input:      "woop woop",
			shouldFail: true,
		},
		{
			name:                "Good protobuf",
			input:               "package spotify.protoman;",
			expectedPackageName: "spotify.protoman",
			shouldFail:          false,
		},
	}
	for _, tc := range tt {
		t.Run(fmt.Sprintf(tc.name), func(t *testing.T) {
			packageName, err := getPackageName(strings.NewReader(tc.input))
			if err != nil && !tc.shouldFail {
				t.Fatal("Expected test to pass")
			}

			if tc.expectedPackageName != packageName {
				t.Fatalf("Expected package name %s; got %s", tc.expectedPackageName, packageName)
			}

		})
	}
}
