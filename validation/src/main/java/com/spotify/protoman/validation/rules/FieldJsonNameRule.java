package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

public class FieldJsonNameRule implements ValidationRule {

  private FieldJsonNameRule() {
  }

  public static FieldJsonNameRule create() {
    return new FieldJsonNameRule();
  }

  @Override
  public void fieldChanged(final Context ctx,
                           final FieldDescriptor current,
                           final FieldDescriptor candidate) {
    if (!Objects.equals(candidate.jsonName(), current.jsonName())) {
      // TODO(staffan): Should we use different levels to indicate breaking binary, json and text
      // encoding? (the latter we don't warn for at all currently)
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          String.format("json name changed (%s -> %s)", current.jsonName(), candidate.jsonName())
      );
    }
  }
}
