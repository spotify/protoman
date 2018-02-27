package path

import (
	"testing"
)

func TestPath(t *testing.T) {
	paths, err := FindProtoFiles("../fixtures")
	if err != nil {
		t.Fatal(err)
	}
	if len(paths) != 3 {
		t.Errorf("Expected 3 protos in fixtures, got; %d", len(paths))
	}
}
