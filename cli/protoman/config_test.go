package protoman

import (
	"os"
	"testing"
)

func TestConfig(t *testing.T) {
	config := config{configPath: "."}
	err := config.Init("spotify/foobar", "src/main/proto")
	if err != nil {
		t.Fatal(err)
	}
	defer os.Remove(".protoman.yaml")

	err = config.AddPackage("github.com/foobar", "3rd_party")
	if err != nil {
		t.Fatal(err)
	}
	err = config.AddPackage("github.com/winning", "3rd_party")
	if err != nil {
		t.Fatal(err)
	}

	if len(config.GetPackages("3rd_party")) != 2 {
		t.Error("Expected to find two third party packages")
	}

	if len(config.GetPackages("local")) != 1 {
		t.Error("Expected to find 1 local package")
	}

	if config.Read() != nil {
		t.Error("failed to read config")
	}
}
