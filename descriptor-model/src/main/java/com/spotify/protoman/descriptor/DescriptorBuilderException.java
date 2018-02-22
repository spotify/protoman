package com.spotify.protoman.descriptor;

public class DescriptorBuilderException extends Exception {

  DescriptorBuilderException(final String message) {
    super(message);
  }

  DescriptorBuilderException(final String message, final Throwable cause) {
    super(message, cause);
  }

  DescriptorBuilderException(final Throwable cause) {
    super(cause);
  }
}
