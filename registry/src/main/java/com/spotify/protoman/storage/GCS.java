package com.spotify.protoman.storage;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class GCS {

  private final Storage gcs;
  private final String bucket;

  public GCS(String bucket) {
    this.bucket = bucket;
    this.gcs = StorageOptions.getDefaultInstance().getService();
  }

  public void get(String path) {
  }

  public void put(String path, Byte content) {
    gcs.create(BlobInfo.newBuilder(bucket, path)
        .build(), "foobar".getBytes());
  }
}
