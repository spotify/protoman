package com.spotify.protoman.validation;

public interface ValidationContext {

  void report(ViolationType type, String description);
}
