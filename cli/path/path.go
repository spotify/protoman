package path

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/pkg/errors"
	"github.com/spotify/protoman/cli/registry"
)

// FindProtoFiles return list over all .proto files under given directory
func FindProtoFiles(rootPath string) ([]*registry.ProtoFile, error) {
	protoFiles := []*registry.ProtoFile{}

	err := filepath.Walk(rootPath, func(path string, info os.FileInfo, err error) error {
		if info.IsDir() {
			return nil
		}
		if filepath.Ext(path) == ".proto" {
			protoFiles = append(protoFiles, &registry.ProtoFile{Path: path})
		}
		return nil
	})
	if err != nil {
		errors.Wrap(err, "Error traversing directory "+rootPath)
	}
	fmt.Printf("Found %v proto schema file(s)\n", len(protoFiles))
	return protoFiles, nil
}
