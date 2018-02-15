package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

public class JavaPackageRule implements ValidationRule {

  private JavaPackageRule() {
  }

  public static JavaPackageRule create() {
    return new JavaPackageRule();
  }

  @Override
  public void fileAdded(final Context ctx, final FileDescriptor candidate) {
    validateJavaPackageOptionSet(ctx, candidate);
  }

  @Override
  public void fileChanged(
      final Context ctx, final FileDescriptor current, final FileDescriptor candidate) {
    validateJavaPackageOptionSet(ctx, candidate);

    // If java_package is set in the current version and has been changed, that will break
    // existing users of the generated code.
    if (current.options().hasJavaPackage() &&
        !Objects.equals(current.javaPackage(), candidate.javaPackage())) {
      // TODO: Should reference line where the java_package option is defined in output
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "Java package changed"
      );
    }
  }

  private void validateJavaPackageOptionSet(final Context ctx, final FileDescriptor descriptor) {
    if (descriptor.options().hasJavaPackage()) {
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "'java_package' option should be set"
      );
    }
  }
}
