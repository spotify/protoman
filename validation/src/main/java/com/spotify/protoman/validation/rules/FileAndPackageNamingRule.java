package com.spotify.protoman.validation.rules;

import com.google.common.base.Joiner;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class FileAndPackageNamingRule implements ValidationRule {

  private FileAndPackageNamingRule() {
  }

  public static FileAndPackageNamingRule create() {
    return new FileAndPackageNamingRule();
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
          "package should always be set"
      );
    } else {
      // Package name should be all lower-case
      if (!Objects.equals(candidate.protoPackage().toLowerCase(), candidate.protoPackage())) {
        ctx.report(
            ViolationType.STYLE_GUIDE_VIOLATION,
            "package should be all-lower case"
        );
      }

      // Package name should match file path
      if (!filePathMatchesPackage(candidate)) {
        ctx.report(
            ViolationType.BEST_PRACTICE_VIOLATION,
            "proto file path should match package"
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

      // TODO(staffan): Make configurable
      /*if (!candidate.getPackage().startsWith("spotify.")) {
        violations.add(
            styleGuideViolation("package name should start with 'spotify.'", candidate)
        );
      }*/
    }
  }

  private boolean filePathMatchesPackage(final FileDescriptor descriptor) {
    final Path parent = Paths.get(descriptor.file().name()).getParent();
    return parent != null && Objects.equals(Joiner.on(".").join(parent), descriptor.protoPackage());
  }
}
