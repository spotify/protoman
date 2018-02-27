package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;

public class PackageRequiredRule implements ValidationRule {

  private PackageRequiredRule() {
  }

  public static PackageRequiredRule create() {
    return new PackageRequiredRule();
  }

  @Override
  public void fileAdded(final Context ctx, final FileDescriptor candidate) {
    validatePackageName(ctx, candidate);
  }

  @Override
  public void fileChanged(final Context ctx,
                          final FileDescriptor current,
                          final FileDescriptor candidate) {
    validatePackageName(ctx, candidate);
  }

  private void validatePackageName(final Context ctx,
                                   final FileDescriptor candidate) {
    if (candidate.protoPackage().isEmpty()) {
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "package must always be set"
      );
    }
  }
}
