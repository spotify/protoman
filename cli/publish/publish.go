package publish

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/spotify/protoman/cli/validator"
)

// GetProtos in directory
func GetProtos(rootPath string) []string {
	list := []string{}
	err := filepath.Walk(rootPath, func(path string, info os.FileInfo, err error) error {
		if info.IsDir() {
			return nil
		}
		if filepath.Ext(path) == ".proto" {
			list = append(list, path)
		}
		return nil
	})
	if err != nil {
		fmt.Printf("walk error [%v]\n", err)
	}
	return list
}

// Publish protobufs
func Publish(rootPath string) {
	fmt.Println("Publishing started...")
	for _, proto := range GetProtos(rootPath) {
		err := validator.ValidateProto(proto)
		if err != nil {
			fmt.Printf("%s is invalid: %v\n", proto, err)
			continue
		}
	}
}
