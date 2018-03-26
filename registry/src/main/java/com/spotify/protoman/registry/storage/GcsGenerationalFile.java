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

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Helper class that represents a versioned file in GCS with optimistic locking semantics
 */
public class GcsGenerationalFile {

  private static final int GCS_PRECONDITION_FAILED_CODE = 412;

  private final Storage storage;
  private final String bucket;
  private final String path;
  private AtomicLong generation;

  private GcsGenerationalFile(final Storage storage,
                              final String bucket,
                              final String path) {

    this.storage = Objects.requireNonNull(storage);
    this.bucket = Objects.requireNonNull(bucket);
    this.path = Objects.requireNonNull(path);

    generation = new AtomicLong();
  }

  public static GcsGenerationalFile create(final Storage storage,
                                           final String bucket,
                                           final String path) {

    return new GcsGenerationalFile(storage, bucket, path);
  }

  /**
   * Create the file if it does not exist in the bucket.
   *
   * @return true if the file was created
   */
  public boolean createIfNotExists(byte[] bytes) {
    try {
      storage.create(
          BlobInfo.newBuilder(bucket, path).build(),
          bytes,
          Storage.BlobTargetOption.doesNotExist()
      );
      return true;
    } catch (StorageException ex) {
      // allow precondition failed, which means we already have a file
      if (ex.getCode() != GCS_PRECONDITION_FAILED_CODE) {
        throw ex;
      }
    }
    return false;
  }

  /**
   * Replace the contents of the file iff it has not been modified by anyone else since it was
   * loaded
   *
   * @return the new generation of the file
   *
   * @throws OptimisticLockingException if the underlying file content has been modified
   */
  public long replace(byte[] bytes) {
    Preconditions.checkState(generation.get() != 0L, "File is not loaded.");

    try {
      final Blob blob = storage.create(
          BlobInfo.newBuilder(bucket, path, generation.get()).build(),
          bytes,
          Storage.BlobTargetOption.generationMatch()
      );
      generation.set(blob.getGeneration());
      return generation.get();
    } catch (StorageException ex) {
      // allow precondition failed, which means we already have a file
      if (ex.getCode() != GCS_PRECONDITION_FAILED_CODE) {
        throw ex;
      }
      throw new OptimisticLockingException("Index file has been modified.", ex);
    }
  }

  public byte[] load() {
    final Blob blob = storage.get(BlobId.of(bucket, path));
    if (blob == null) {
      throw new NotFoundException("File not found.");
    }
    generation.set(blob.getGeneration());
    return blob.getContent();
  }

  public byte[] contentForGeneration(long generation) {
    final Blob blob = storage.get(BlobId.of(bucket, path, generation));
    if (blob == null) {
      throw new NotFoundException("File with generation " + generation + " not found.");
    }
    return blob.getContent();
  }

  public long currentGeneration() {
    Preconditions.checkState(generation.get() != 0L, "File is not loaded.");

    return generation.get();
  }

  public Stream<Long> listGenerations() {
    final Page<Blob> blobPage = storage.list(
        bucket,
        Storage.BlobListOption.prefix(path),
        Storage.BlobListOption.versions(true)
    );

    return StreamSupport.stream(blobPage.iterateAll().spliterator(), false)
        .filter(b -> b.getName().equals(path))
        .map(Blob::getGeneration);
  }

  public static class NotFoundException extends RuntimeException {

    private NotFoundException(final String message) {
      super(message);
    }
  }

  public static class OptimisticLockingException extends RuntimeException {

    private OptimisticLockingException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
