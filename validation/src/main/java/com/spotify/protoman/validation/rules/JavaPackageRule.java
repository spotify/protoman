package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

public class JavaPackageRule implements ComparingValidationRule {

  private JavaPackageRule() {
  }

  public static JavaPackageRule create() {
    return new JavaPackageRule();
  }

  @Override
  public void fileAdded(final ValidationContext ctx, final FileDescriptor candidate) {
    validateJavaPackageOptionSet(ctx, candidate);
  }

  @Override
  public void fileChanged(
      final ValidationContext ctx, final FileDescriptor current, final FileDescriptor candidate) {
    validateJavaPackageOptionSet(ctx, candidate);

    if (!Objects.equals(current.javaPackage(), candidate.javaPackage())) {
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "Java package changed"
      );
    }
  }

  private void validateJavaPackageOptionSet(final ValidationContext ctx, final FileDescriptor descriptor) {
    if (!descriptor.options().hasJavaPackage()) {
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "java_package option should be set"
      );
    }
  }
}
