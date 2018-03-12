package protoman

import (
	"os"
	"testing"
)

func TestConfig(t *testing.T) {
	config := config{}
	defer os.Remove(".protoman")

	err := config.AddThirdPartyPackage("github.com/foobar")
	if err != nil {
		t.Fatal(err)
	}
	err = config.AddThirdPartyPackage("github.com/winning")
	if err != nil {
		t.Fatal(err)
	}
	cfg, err := config.read()
	if err != nil {
		t.Error(err)
	}
	if len(cfg.ThirdParty) != 2 {
		t.Error("Expected to find two third party packages")
	}
	err = config.AddLocalPackage("spotify.com/foobar")
	if err != nil {
		t.Fatal(err)
	}
	cfg, err = config.read()
	if err != nil {
		t.Error(err)
	}
	if len(cfg.Local) != 1 {
		t.Error("Expected to find 1 local package")
	}

}
