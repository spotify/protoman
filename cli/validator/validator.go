package validator

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"

	"github.com/emicklei/proto"
	"github.com/pkg/errors"
)

// getPackageName from proto
func getPackageName(r io.Reader) (string, error) {
	parser := proto.NewParser(r)
	def, err := parser.Parse()
	if err != nil {
		return "", err
	}
	for _, element := range def.Elements {
		switch element := element.(type) {
		case *proto.Package:
			return element.Name, nil
		}
	}
	return "", fmt.Errorf("Package name not found")
}

// ValidateProto validates a .proto file
func ValidateProto(filePath string) error {
	f, err := os.Open(filePath)
	if err != nil {
		return errors.Wrap(err, "Failed to open "+filePath)
	}
	defer f.Close()
	packageName, err := getPackageName(f)
	if err != nil {
		return errors.Wrap(err, "Unable to parse proto file")
	}
	dir := filepath.Dir(filePath)
	packagePath := strings.Replace(packageName, ".", "/", -1)
	if !strings.HasSuffix(dir, packagePath) {
		return fmt.Errorf("Package path does not match filepath, expected directory path to end with " + packagePath)
	}
	return nil
}
