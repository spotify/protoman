package parser

import (
	"fmt"
	"io"

	"github.com/emicklei/proto"
)

// GetPackage from proto
func GetPackage(r io.Reader) (string, error) {
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
