package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

public class PackageNamingRule implements ComparingValidationRule {

  private PackageNamingRule() {
  }

  public static PackageNamingRule create() {
    return new PackageNamingRule();
  }

  @Override
  public void fileAdded(final ValidationContext ctx, final FileDescriptor candidate) {
    validatePackageName(ctx, candidate);
  }

  @Override
  public void fileChanged(final ValidationContext ctx,
                          final FileDescriptor current,
                          final FileDescriptor candidate) {
    validatePackageName(ctx, candidate);
  }

  private void validatePackageName(final ValidationContext ctx,
                                   final FileDescriptor candidate) {
    if (candidate.protoPackage().isEmpty()) {
      return;
    }

    // Package name should be all lower-case
    if (!Objects.equals(candidate.protoPackage().toLowerCase(), candidate.protoPackage())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "package name should be all-lower case"
      );
    }

    // Package name should not contain "schema" or "proto"
    final String lowerCasePackage = candidate.protoPackage().toLowerCase();
    if (lowerCasePackage.endsWith(".proto") || lowerCasePackage.contains(".proto.")
        || lowerCasePackage.endsWith(".schema") || lowerCasePackage.contains(".schema.")) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "package name should not contain 'schema' or 'proto'"
      );
    }
  }
}
