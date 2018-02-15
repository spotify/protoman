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
