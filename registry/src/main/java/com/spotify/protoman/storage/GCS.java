package com.spotify.protoman.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class GCS {

  private final Storage storage;
  private final String bucket;

  public GCS(String bucket) {
    this.bucket = bucket;
    this.storage = StorageOptions.getDefaultInstance().getService();
  }

  public byte[] get(String path) {

    Blob blob = storage.get(BlobId.of(bucket, path));
    if (blob == null) {
      throw new RuntimeException(path +" not found");
    }
    return blob.getContent();
  }

  public void put(String path, byte[] content) {
    storage.create(BlobInfo.newBuilder(bucket, path)
        .setContentType("text/plain")
        .build(), content);
  }
}
