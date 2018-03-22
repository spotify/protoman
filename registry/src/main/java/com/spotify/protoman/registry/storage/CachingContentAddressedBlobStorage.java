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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashCode;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
// TODO: replace guava cache with local disk caching
public class CachingContentAddressedBlobStorage implements ContentAddressedBlobStorage {

  private final LoadingCache<HashCode, byte[]> cache;
  private final ContentAddressedBlobStorage delegate;

  private CachingContentAddressedBlobStorage(final ContentAddressedBlobStorage delegate) {
    this.delegate = Objects.requireNonNull(delegate);
    cache = CacheBuilder.newBuilder()
        .maximumSize(100_000)
        .build(new CacheLoader<HashCode, byte[]>() {
          @Override
          public byte[] load(final HashCode key) throws Exception {
            return delegate.get(key)
                .orElseThrow(() -> new IOException("Not found: " + key));
          }
        });
  }

  public static ContentAddressedBlobStorage create(
      ContentAddressedBlobStorage contentAddressedBlobStorage) {
    return new CachingContentAddressedBlobStorage(contentAddressedBlobStorage);

  }

  @Override
  public HashCode put(final byte[] bytes) {
    return delegate.put(bytes);
  }

  @Override
  public Optional<byte[]> get(final HashCode contentHash) {
    try {
      return Optional.ofNullable(cache.get(contentHash));
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }
}
