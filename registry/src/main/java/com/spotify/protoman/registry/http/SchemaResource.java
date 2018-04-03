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

import com.google.protobuf.DescriptorProtos;
import com.spotify.protoman.registry.SchemaRegistry;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("")
public final class SchemaResource {

  private SchemaRegistry schemaRegistry;

  private SchemaResource(final SchemaRegistry schemaRegistry) {
    this.schemaRegistry = schemaRegistry;
  }

  public static SchemaResource create(final SchemaRegistry schemaRegistry) {
    return new SchemaResource(schemaRegistry);
  }

  @Path("/descriptors")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public DescriptorProtos.FileDescriptorSet fetchAllDescriptors() {

    return schemaRegistry.allDescriptors();
  }

//  @Path("/events")
//  @GET
//  @Produces(MediaType.APPLICATION_JSON)
//  public EventList fetchAllEvents() {
//    final EventList.Builder builder = EventList.newBuilder();
//
//    try (SchemaStorage.Transaction tx = schemaStorage.open();
//         Stream<Event> allEvents = tx.fetchAllEvents()) {
//      allEvents.forEach(builder::addEvent);
//    }
//
//    return builder.build();
//  }
}
