package protoman

import (
	"io/ioutil"
	"os"
	"testing"
)

func TestAddThirdPartyPackages(t *testing.T) {
	tmpfile, err := ioutil.TempFile("", "protoman")
	if err != nil {
		t.Fatal(err)
	}
	defer os.Remove(tmpfile.Name()) // clean up

	// Override default config for this test only.
	defaultConfig = tmpfile.Name()

	tests := []struct {
		pkgs            []*protoPackage
		expextedEntries int
	}{
		{
			pkgs: []*protoPackage{
				&protoPackage{
					Path: "src/main/proto/test/foobar",
					Pkg:  "test.foobar",
				},
			},
			expextedEntries: 1,
		},
		{
			pkgs: []*protoPackage{
				&protoPackage{
					Path: "src/main/proto/test/foobar",
					Pkg:  "test.foobar",
				},
				&protoPackage{
					Path: "src/main/proto/test/foobar/v1",
					Pkg:  "test.foobar.v1",
				},
			},
			expextedEntries: 2,
		},
		{
			pkgs: []*protoPackage{
				&protoPackage{
					Path: "src/main/proto/test/foobar",
					Pkg:  "test.foobar",
				},
				&protoPackage{
					Path: "src/main/proto/test/foobar/v1",
					Pkg:  "test.foobar.v1",
				},
				&protoPackage{
					Path: "src/main/proto/test/foobar/v1",
					Pkg:  "test.foobar.v1",
				},
			},
			expextedEntries: 2,
		},
	}

	for _, test := range tests {
		for _, pkg := range test.pkgs {
			if err := addThirdPartyPackage(pkg); err != nil {
				t.Fatal(err)
			}
		}

		cfg, err := readConfig()
		if err != nil {
			t.Error(err)
		}
		if len(cfg.ThirdParty) != test.expextedEntries {
			t.Errorf("Expected to find %v third_party packages, got %v", test.expextedEntries, len(cfg.ThirdParty))
		}
	}
}

func TestAddLocalPackages(t *testing.T) {
	tmpfile, err := ioutil.TempFile("", "protoman")
	if err != nil {
		t.Fatal(err)
	}
	defer os.Remove(tmpfile.Name()) // clean up

	// Override default config for this test only.
	defaultConfig = tmpfile.Name()

	tests := []struct {
		pkgs            []*protoPackage
		expextedEntries int
	}{
		{
			pkgs: []*protoPackage{
				&protoPackage{
					Path: "src/main/proto/test/foobar",
					Pkg:  "test.foobar",
				},
			},
			expextedEntries: 1,
		},
		{
			pkgs: []*protoPackage{
				&protoPackage{
					Path: "src/main/proto/test/foobar",
					Pkg:  "test.foobar",
				},
				&protoPackage{
					Path: "src/main/proto/test/foobar/v1",
					Pkg:  "test.foobar.v1",
				},
			},
			expextedEntries: 2,
		},
		{
			pkgs: []*protoPackage{
				&protoPackage{
					Path: "src/main/proto/test/foobar",
					Pkg:  "test.foobar",
				},
				&protoPackage{
					Path: "src/main/proto/test/foobar/v1",
					Pkg:  "test.foobar.v1",
				},
				&protoPackage{
					Path: "src/main/proto/test/foobar/v1",
					Pkg:  "test.foobar.v1",
				},
			},
			expextedEntries: 2,
		},
	}

	for _, test := range tests {
		for _, pkg := range test.pkgs {
			if err := addLocalPackage(pkg); err != nil {
				t.Fatal(err)
			}
		}

		cfg, err := readConfig()
		if err != nil {
			t.Error(err)
		}
		if len(cfg.Local) != test.expextedEntries {
			t.Errorf("Expected to find %v third_party packages, got %v", test.expextedEntries, len(cfg.Local))
		}
	}
}
