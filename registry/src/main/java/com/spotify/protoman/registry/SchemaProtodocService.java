package com.spotify.protoman.registry;

import com.spotify.protoman.SchemaProtodocGrpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaProtodocService extends SchemaProtodocGrpc.SchemaProtodocImplBase {
  private static final Logger logger = LoggerFactory.getLogger(SchemaProtodocService.class);

  private SchemaProtodocService() {
  }

  public static SchemaProtodocService create() {
    return new SchemaProtodocService();
  }
}
