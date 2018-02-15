package com.spotify.protoman.descriptor;

import javax.annotation.Nullable;

class Util {

  private Util() {
  }

  static String computeFullName(final FileDescriptor file,
                                @Nullable final MessageDescriptor parent,
                                final String name) {
    if (parent != null) {
      return parent.fullName() + '.' + name;
    } else if (file.protoPackage().length() > 0) {
      return file.protoPackage() + '.' + name;
    } else {
      return name;
    }
  }
}
