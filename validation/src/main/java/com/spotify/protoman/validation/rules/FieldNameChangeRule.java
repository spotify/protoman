package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

public class FieldNameChangeRule implements ComparingValidationRule {

  private FieldNameChangeRule() {
  }

  public static FieldNameChangeRule create() {
    return new FieldNameChangeRule();
  }

  @Override
  public void fieldChanged(final ValidationContext ctx,
                           final FieldDescriptor current,
                           final FieldDescriptor candidate) {
    if (!Objects.equals(current.name(), candidate.name())) {
      // TODO(staffan): Will break generated source code as well as FieldMask usage. How to
      // signal this?
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "field name changed"
      );
    }
  }
}
