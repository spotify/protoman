/*-
 * -\-\-
 * protoman-descriptor-model
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

package com.spotify.protoman.descriptor;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Information about a descriptor definition from the raw proto file, including where it was
 * defined (file, line, column) and any comments in the original proto.
 */
@AutoValue
public abstract class SourceCodeInfo {

  public abstract Path filePath();

  public abstract FilePosition start();

  public abstract FilePosition end();

  public abstract String leadingComments();

  public abstract String trailingComments();

  public abstract ImmutableList<String> detachedLeadingComments();

  public static FilePosition startPosition(
      final DescriptorProtos.SourceCodeInfo.Location location) {
    // Line and column numbers are 0-based in the SourceCodeInfo proto - add one to make them
    // more human-friendly
    return SourceCodeInfo.FilePosition.create(
        1 + location.getSpan(0),
        1 + location.getSpan(1)
    );
  }

  public static FilePosition endPosition(
      final DescriptorProtos.SourceCodeInfo.Location location) {
    // Line and column numbers are 0-based in the SourceCodeInfo proto - add one to make them
    // more human-friendly
    return location.getSpanCount() == 3 ? SourceCodeInfo.FilePosition.create(
        1 + location.getSpan(0),
        1 + location.getSpan(2)
    ) : SourceCodeInfo.FilePosition.create(
        1 + location.getSpan(2),
        1 + location.getSpan(3)
    );
  }

  public static SourceCodeInfo create(final Path filePath,
                                      final DescriptorProtos.SourceCodeInfo.Location location) {
    return SourceCodeInfo.create(
        filePath,
        startPosition(location),
        endPosition(location),
        location.getLeadingComments(),
        location.getTrailingComments(),
        location.getLeadingDetachedCommentsList().stream()
    );
  }

  public static SourceCodeInfo create(
      final Path filePath,
      final FilePosition start, final FilePosition end,
      final String leadingComments, final String trailingComments,
      final Stream<String> detachedLeadingComments) {
    return new AutoValue_SourceCodeInfo(
        filePath, start, end, leadingComments, trailingComments,
        detachedLeadingComments.collect(toImmutableList())
    );
  }

  @AutoValue
  public static abstract class FilePosition {

    public abstract int line();

    public abstract int column();

    public static FilePosition create(final int line, final int column) {
      return new AutoValue_SourceCodeInfo_FilePosition(line, column);
    }
  }
}
