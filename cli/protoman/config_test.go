package protoman

import (
	"os"
	"testing"
)

func TestConfig(t *testing.T) {
	defer os.Remove(".protoman")

	err := addThirdPartyPackage(&protoPackage{Pkg: "test.foobar", Path: "src/main/proto/test/foobar"})
	if err != nil {
		t.Fatal(err)
	}
	err = addThirdPartyPackage(&protoPackage{Pkg: "test.winning", Path: "src/main/proto/test/winning"})
	if err != nil {
		t.Fatal(err)
	}
	cfg, err := readConfig()
	if err != nil {
		t.Error(err)
	}
	if len(cfg.ThirdParty) != 2 {
		t.Error("Expected to find two third party packages")
	}
	err = addLocalPackage(&protoPackage{Pkg: "test.local", Path: "src/main/proto/test/local"})
	if err != nil {
		t.Fatal(err)
	}
	cfg, err = readConfig()
	if err != nil {
		t.Error(err)
	}
	if len(cfg.Local) != 1 {
		t.Error("Expected to find 1 local package")
	}

}
