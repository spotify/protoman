package com.spotify.protoman.registry;

import com.google.cloud.storage.Bucket;
import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;
import com.spotify.protoman.ProtoFile;
import java.util.stream.Stream;

public class GcsProtoStore implements ProtoStore {

  private final Bucket bucket;

  private GcsProtoStore(final Bucket bucket) {
    this.bucket = bucket;
  }

  public static GcsProtoStore create(final Bucket bucket) {
    return new GcsProtoStore(bucket);
  }

  @Override
  public Stream<ProtoFile> getAll() {
    return Streams.stream(bucket.list().iterateAll())
        .map(blob -> {
          final byte[] content = blob.getContent();
          final String name = blob.getName();
          return ProtoFile.newBuilder()
              .setContent(ByteString.copyFrom(content))
              .setPath(name)
              .build();
        });
  }

  @Override
  public void store(final Stream<ProtoFile> protoFiles) {
    protoFiles.forEach(
        protoFile -> bucket.create(protoFile.getPath(), protoFile.getContent().toByteArray())
    );
  }
}
