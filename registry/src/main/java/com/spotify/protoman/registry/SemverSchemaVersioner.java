/*-
 * -\-\-
 * protoman-registry
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.descriptor.FileDescriptor;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Versions schemas according to semantic versioning, with some caveats.
 *
 * The major versions is always derived from the package. E.g. foo.bar.v2 and foo.bar are
 * assigned major version 2 and 1 respectively. The versioner does not prevent breaking changes
 * and does not bump the major version in case of breaking changes - that has to be handled
 * elsewhere.
 *
 * Any type additions or changes cause the minor version to increased. Comment and formatting
 * changes cause the patch version to be increased.
 *
 * Changing the ordering of definitions in a proto file will cause the minor version to be bumped,
 * even though technically nothing was changed. For example, if you change the order in which 2
 * fields in a message are defined.
 */
public class SemverSchemaVersioner implements SchemaVersioner {

  private static final Pattern MAJOR_VERSION_RE =
      Pattern.compile(".*\\.v(?<major>\\d+(?:alpha\\d*|beta\\d*)?)$");

  private SemverSchemaVersioner() {
  }

  public static SemverSchemaVersioner create() {
    return new SemverSchemaVersioner();
  }

  @Override
  public SchemaVersion determineVersion(final String protoPackage,
                                        @Nullable final SchemaVersion currentVersion,
                                        final DescriptorSet current,
                                        final DescriptorSet candidate) {
    final String majorVersion = getMajorVersion(protoPackage);
    if (currentVersion == null) {
      return SchemaVersion.create(majorVersion, 0, 0);
    }
    Preconditions.checkState(Objects.equals(majorVersion, currentVersion.major()));

    if (hasMinorVersionChange(protoPackage, current, candidate)) {
      final int minorVersion = currentVersion.minor() + 1;
      return SchemaVersion.create(majorVersion, minorVersion, 0);
    }

    if (hasPatchVersionChange(protoPackage, current, candidate)) {
      final int patchVersion = currentVersion.patch() + 1;
      return SchemaVersion.create(majorVersion, currentVersion.minor(), patchVersion);
    }

    return currentVersion;
  }

  @VisibleForTesting
  static String getMajorVersion(final String protoPackage) {
    final Matcher matcher = MAJOR_VERSION_RE.matcher(protoPackage);
    if (matcher.matches()) {
      return matcher.group("major");
    } else {
      return "1";
    }
  }

  /**
   * File additions, removals and renames are considered minor version changes. As are any
   * formatting/comment changes to proto files. I.e. adding a type, adding a field, etc. are
   * considered minor version changes.
   */
  private static boolean hasMinorVersionChange(final String protoPackage,
                                               final DescriptorSet current,
                                               final DescriptorSet candidate) {
    final Predicate<FileDescriptor> predicate =
        fileDescriptor -> Objects.equals(fileDescriptor.protoPackage(), protoPackage);

    final ImmutableMap<String, FileDescriptor> currentFileDescriptors =
        current.fileDescriptors().stream()
            .filter(predicate)
            .collect(toImmutableMap(FileDescriptor::fullName, Function.identity()));

    final ImmutableMap<String, FileDescriptor> candidateFileDescriptors =
        candidate.fileDescriptors().stream()
            .filter(predicate)
            .collect(toImmutableMap(FileDescriptor::fullName, Function.identity()));

    if (!Objects.equals(currentFileDescriptors.keySet(), candidateFileDescriptors.keySet())) {
      // .proto files were added or removed
      return true;
    }

    // Did any of the descriptors change?
    // We consider the descriptors equal if at most SourceCodeInfo differs between the two
    // descriptors. I.e. formatting changes are usually not considered minor version changes.
    return currentFileDescriptors.keySet().stream()
        .anyMatch(key -> {
          // No files were added or removed, thus currentFd and candidateFd should always be
          // non-null
          final FileDescriptor currentFd = currentFileDescriptors.get(key);
          final FileDescriptor candidateFd = candidateFileDescriptors.get(key);

          assert currentFd != null && candidateFd != null;

          final DescriptorProtos.FileDescriptorProto currentFdProto = currentFd.toProto()
              .toBuilder()
              .clearSourceCodeInfo()
              .build();
          final DescriptorProtos.FileDescriptorProto candidateFdProto = candidateFd.toProto()
              .toBuilder()
              .clearSourceCodeInfo()
              .build();

          return !Objects.equals(currentFdProto, candidateFdProto);
        });
  }

  private static boolean hasPatchVersionChange(final String protoPackage,
                                               final DescriptorSet current,
                                               final DescriptorSet candidate) {
    final Predicate<FileDescriptor> predicate =
        fileDescriptor -> Objects.equals(fileDescriptor.protoPackage(), protoPackage);

    final ImmutableMap<String, FileDescriptor> currentFileDescriptors =
        current.fileDescriptors().stream()
            .filter(predicate)
            .collect(toImmutableMap(FileDescriptor::fullName, Function.identity()));

    final ImmutableMap<String, FileDescriptor> candidateFileDescriptors =
        candidate.fileDescriptors().stream()
            .filter(predicate)
            .collect(toImmutableMap(FileDescriptor::fullName, Function.identity()));

    return currentFileDescriptors.keySet().stream()
        .anyMatch(key -> {
          final FileDescriptor currentFd = currentFileDescriptors.get(key);
          final FileDescriptor candidateFd = candidateFileDescriptors.get(key);
          assert currentFd != null && candidateFd != null;
          return !Objects.equals(currentFd.toProto(), candidateFd.toProto());
        });
  }
}
