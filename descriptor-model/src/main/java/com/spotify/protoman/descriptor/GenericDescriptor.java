package com.spotify.protoman.descriptor;

import com.google.protobuf.Message;
import java.util.Optional;

public interface GenericDescriptor {

  Message toProto();

  String name();

  String fullName();

  FileDescriptor file();

  Optional<SourceCodeInfo> sourceCodeInfo();
}
