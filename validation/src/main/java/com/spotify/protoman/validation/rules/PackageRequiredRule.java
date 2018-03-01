package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;

public class PackageRequiredRule implements ValidationRule {

  private PackageRequiredRule() {
  }

  public static PackageRequiredRule create() {
    return new PackageRequiredRule();
  }

  @Override
  public void validateFile(final ValidationContext ctx, final FileDescriptor candidate) {
    if (candidate.protoPackage().isEmpty()) {
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "package must always be set"
      );
    }
  }
}
