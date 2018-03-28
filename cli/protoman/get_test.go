package protoman

import (
	"context"
	"fmt"
	"os"
	"testing"

	"github.com/spotify/protoman/cli/registry"
	"google.golang.org/grpc"
)

type mockRegistry struct{}

func (m *mockRegistry) GetSchema(ctx context.Context, in *registry.GetSchemaRequest, opts ...grpc.CallOption) (*registry.GetSchemaResponse, error) {
	return &registry.GetSchemaResponse{
		[]*registry.ProtoFile{
			&registry.ProtoFile{
				Path:    "spotify/foobar/service.proto",
				Content: "proto content",
			},
		}}, nil
}

func (m *mockRegistry) PublishSchema(ctx context.Context, in *registry.PublishSchemaRequest, opts ...grpc.CallOption) (*registry.PublishSchemaResponse, error) {
	return &registry.PublishSchemaResponse{}, nil
}

func mockClient() registry.SchemaRegistryClient {
	return &mockRegistry{}
}

func TestGet(t *testing.T) {

	Get(mockClient(), "test-dir/spotify/foobar/foobar", []string{"spotify.foobar"})
	defer os.RemoveAll("test-dir")
	defer os.Remove(".protoman")
	c, err := readConfig()
	if err != nil {
		t.Fatal("failed to read config", err)
	}
	if len(c.ThirdParty) != 1 {
		fmt.Errorf("expected 1 third party dependency in configuration")
	}
	path := "test-dir/spotify/foobar/service.proto"
	if _, err := os.Stat(path); os.IsNotExist(err) {
		fmt.Errorf("expected %s after get", path)
	}
}
