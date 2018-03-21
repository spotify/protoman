package protoman

import (
	"fmt"
	"strings"
)

// Add protoman schema to config file
func Add(packageName, path string) error {
	if !strings.HasSuffix(path, strings.Replace(packageName, ".", "/", -1)) {
		return fmt.Errorf("package name does not match path, expected path to end with %s", path)
	}
	return addLocalPackage(ProtoPackage{Pkg: packageName, Path: path})
}
