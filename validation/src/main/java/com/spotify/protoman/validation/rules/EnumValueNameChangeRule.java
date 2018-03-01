package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

public class EnumValueNameChangeRule implements ComparingValidationRule {

  private EnumValueNameChangeRule() {
  }

  public static EnumValueNameChangeRule create() {
    return new EnumValueNameChangeRule();
  }

  @Override
  public void enumValueChanged(final ValidationContext ctx,
                               final EnumValueDescriptor current,
                               final EnumValueDescriptor candidate) {
    // TODO(staffan): This will also change the json encoding, which we should signal
    if (!Objects.equals(current.name(), candidate.name())) {
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "enum value name changed"
      );
    }
  }
}
