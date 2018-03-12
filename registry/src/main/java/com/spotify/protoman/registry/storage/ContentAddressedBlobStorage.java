package com.spotify.protoman.registry.storage;

import com.google.common.hash.HashCode;
import java.util.Optional;

public interface ContentAddressedBlobStorage {

  HashCode put(byte[] bytes);

  Optional<byte[]> get(HashCode contentHash);
}
