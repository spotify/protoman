package com.spotify.protoman.registry;

import com.spotify.protoman.ProtoFile;
import java.util.stream.Stream;

public interface ProtoStore {

  Stream<ProtoFile> getAll();

  void store(Stream<ProtoFile> protoFile);
}
