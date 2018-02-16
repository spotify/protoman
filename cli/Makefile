all: proto compile test

proto: 
	protoc -I ../schema/src/main/proto/spotify/protoman/ ../schema/src/main/proto/spotify/protoman/registry.proto --go_out=plugins=grpc:registry

compile: proto
	go build -o build/protoman main.go

test: proto
	go test -v ./...

clean:
	@rm -rf ./build/protoman
