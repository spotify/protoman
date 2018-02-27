package com.spotify.protoman.validation.rules;

import java.util.Objects;

class CaseFormatUtil {

  static boolean isLowerSnakeCase(final String name) {
    return Objects.equals(name.toLowerCase(), name);
  }

  static boolean isUpperCamelCaseName(final String name) {
    return Character.isUpperCase(name.charAt(0))
           && !name.contains("_")
           && (name.length() == 1 || !Objects.equals(name, name.toUpperCase()));
  }

  // For checking enum value names
  static boolean isUpperSnakeCase(final String name) {
    return Objects.equals(name.toUpperCase(), name);
  }
}
