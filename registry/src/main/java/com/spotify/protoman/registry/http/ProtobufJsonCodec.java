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

package com.spotify.protoman.registry.http;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public final class ProtobufJsonCodec implements MessageBodyWriter<Message> {

  private static final JsonFormat.Printer PRINTER =
      JsonFormat.printer().includingDefaultValueFields();

  @Override
  public boolean isWriteable(
      final Class<?> aClass,
      final Type type,
      final Annotation[] annotations,
      final MediaType mediaType) {
    return Message.class.isAssignableFrom(aClass);
  }

  @Override
  public void writeTo(
      final Message message,
      final Class<?> aClass,
      final Type type,
      final Annotation[] annotations,
      final MediaType mediaType,
      final MultivaluedMap<String, Object> multivaluedMap,
      final OutputStream outputStream)
      throws IOException, WebApplicationException {
    try (OutputStreamWriter output = new OutputStreamWriter(outputStream)) {
      PRINTER.appendTo(message, output);
    }
  }

  @Override
  public long getSize(
      final Message message,
      final Class<?> aClass,
      final Type type,
      final Annotation[] annotations,
      final MediaType mediaType) {
    // Deprecated
    return 0;
  }
}
