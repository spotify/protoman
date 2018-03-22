/*-
 * -\-\-
 * protoman-registry
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.protoman.registry.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.Objects;
import java.util.Optional;

public class GcsContentAddressedBlobStorage implements ContentAddressedBlobStorage {

  private static final HashFunction DEFAULT_CONTENT_HASH_FUNCTION = Hashing.sha256();
  private static final String DEFAULT_CONTENT_TYPE = "text/plain";
  private static final String BLOB_NAME_TEMPLATE = "%s/%s/%s/%s.%s";
  private static final HashFunction GCS_UPLOAD_HASH_FUNCTION = Hashing.crc32c();

  private final String contentType;
  private final Storage storage;
  private final String bucket;
  private final String path;
  private final String fileSuffix;
  private final HashFunction hashFunction;

  private GcsContentAddressedBlobStorage(final Storage storage,
                                         final String bucket,
                                         final String path,
                                         final String fileSuffix,
                                         final String contentType,
                                         final HashFunction hashFunction) {

    this.storage = Objects.requireNonNull(storage);
    this.bucket = Objects.requireNonNull(bucket);
    this.path = Objects.requireNonNull(path);
    this.fileSuffix = Objects.requireNonNull(fileSuffix);
    this.contentType = Objects.requireNonNull(contentType);
    this.hashFunction = Objects.requireNonNull(hashFunction);
  }

  public static GcsContentAddressedBlobStorage create(final Storage storage,
                                                      final String bucket,
                                                      final String path,
                                                      final String fileSuffix,
                                                      final String contentType,
                                                      final HashFunction hashFunction) {

    return new GcsContentAddressedBlobStorage(
        storage,
        bucket,
        path,
        fileSuffix,
        contentType,
        hashFunction);
  }

  public static GcsContentAddressedBlobStorage create(final Storage storage,
                                                      final String bucket,
                                                      final String path,
                                                      final String fileSuffix) {

    return new GcsContentAddressedBlobStorage(
        storage,
        bucket,
        path,
        fileSuffix,
        DEFAULT_CONTENT_TYPE,
        DEFAULT_CONTENT_HASH_FUNCTION);
  }

  @Override
  public HashCode put(final byte[] bytes) {
    final HashCode hashCode = hashFunction.hashBytes(bytes);
    final String crc32c = GCS_UPLOAD_HASH_FUNCTION.hashBytes(bytes).toString();
    final String blobName = blobName(hashCode);
    storage.create(BlobInfo.newBuilder(bucket, blobName)
            .setContentType(contentType)
            .setCrc32c(crc32c)
            .build(),
        bytes);
    return hashCode;
  }

  @Override
  public Optional<byte[]> get(final HashCode contentHash) {
    final String blobName = blobName(contentHash);
    final Blob blob = storage.get(BlobId.of(bucket, blobName));
    return Optional.ofNullable(blob.getContent());
  }

  private String blobName(final HashCode hashCode) {
    final String hashCodeString = hashCode.toString();
    Preconditions.checkState(hashCodeString.length() > 4);
    return String.format(
        BLOB_NAME_TEMPLATE,
        path,
        hashCodeString.substring(0, 2),
        hashCodeString.substring(0, 4),
        hashCodeString,
        fileSuffix);
  }
}
