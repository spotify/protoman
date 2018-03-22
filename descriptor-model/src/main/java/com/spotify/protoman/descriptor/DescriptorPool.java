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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class DescriptorPool {

  // Fully-qualified (.<package>.<type>...) name -> MessageDescriptor
  private final Map<String, MessageDescriptor> messageTypes = new HashMap<>();
  private final Map<String, EnumDescriptor> enumTypes = new HashMap<>();

  private DescriptorPool() {
  }

  public static DescriptorPool create() {
    return new DescriptorPool();
  }

  MessageDescriptor findMessageType(final String fullName) {
    return messageTypes.get(fullyQualify(fullName));
  }

  EnumDescriptor findEnumType(final String fullName) {
    return enumTypes.get(fullyQualify(fullName));
  }

  void put(final MessageDescriptor descriptor) {
    messageTypes.put("." + descriptor.fullName(), descriptor);
  }

  void put(final EnumDescriptor descriptor) {
    enumTypes.put("." + descriptor.fullName(), descriptor);
  }

  public MessageDescriptor lookupMessageDescriptor(final String name,
                                                   final MessageDescriptor relativeTo) {
    return lookupDescriptor(name, relativeTo, this::findMessageType);
  }

  public EnumDescriptor lookupEnumDescriptor(final String name,
                                             final MessageDescriptor relativeTo) {
    return lookupDescriptor(name, relativeTo, this::findEnumType);
  }

  // TODO(staffan): This is probably buggy as hell. protoc seems to produce names that are
  // fully-qualified so much of this code hasn't yet been exercised.
  private static <DescriptorType extends GenericDescriptor>
  DescriptorType lookupDescriptor(final String name, final MessageDescriptor relativeTo,
                                  final Function<String, DescriptorType> lookupFn) {
    if (isFullyQualifiedName(name)) {
      return lookupFn.apply(name);
    }

    // Defined in nested type? E.g.
    //
    // message A {
    //    B b = 1;
    //    B.C c = 2;
    //    message B {
    //      message C {}
    //    }
    // }
    final Deque<MessageDescriptor> q = new ArrayDeque<>();
    q.push(relativeTo);

    while (!q.isEmpty()) {
      final MessageDescriptor d = q.pop();
      final String scope = "." + d.fullName();
      final DescriptorType maybeDesc = lookupFn.apply(scope + "." + name);
      if (maybeDesc != null) {
        return maybeDesc;
      }
      d.nestedTypes().forEach(nestedType -> q.push(d));
    }

    String s = "." + relativeTo.fullName();
    while (true) {
      final int i = s.lastIndexOf('.');
      if (i == -1) {
        return null;
      }
      s = s.substring(0, i);
      final DescriptorType d = lookupFn.apply(s + "." + name);
      if (d != null) {
        return d;
      }
    }
  }

  private static boolean isFullyQualifiedName(final String name) {
    return name.startsWith(".");
  }

  private static String fullyQualify(final String fullName) {
    return isFullyQualifiedName(fullName) ? fullName : "." + fullName;
  }
}
