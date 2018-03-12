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
