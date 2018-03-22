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

import com.google.auto.value.AutoValue;
import com.google.protobuf.DescriptorProtos;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface DescriptorBuilder extends AutoCloseable {


  DescriptorBuilder setProtoFile(Path path, String content) throws DescriptorBuilderException;

  /**
   * Can safely be called multiple times (you can call addProtoFile inbetween).
   */
  Result buildDescriptor(Stream<Path> paths) throws DescriptorBuilderException;

  @Override
  void close();

  interface Factory {

    DescriptorBuilder newDescriptorBuilder();
  }

  @AutoValue
  abstract class Result {

    @Nullable public abstract DescriptorProtos.FileDescriptorSet fileDescriptorSet();

    // TODO(staffan): Change this into a collection of more structured types?
    @Nullable public abstract String compilationError();

    public static Result create(final DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
      return new AutoValue_DescriptorBuilder_Result(fileDescriptorSet, null);
    }

    public static Result error(final String compilationError) {
      return new AutoValue_DescriptorBuilder_Result(null, compilationError);
    }
  }
}
