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

package com.spotify.protoman.registry.storage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import org.junit.Before;

/**
 * Integration-test base class that will assume that a GCS bucket can be
 * reached and ignore tests if not
 */
public class GcsBucketIntegrationTestBase {

  final Storage storage;
  final String bucket;
  final boolean versioned;

  public GcsBucketIntegrationTestBase(final Storage storage,
                                      final String bucket,
                                      final boolean versioned) {
    this.storage = storage;
    this.bucket = bucket;
    this.versioned = versioned;

  }

  @Before
  public void preSetup() {
    preconditions();
  }

  // if these assumptions are not true, the test will be ignored
  private void preconditions() {
    Bucket bucketInfo = null;
    StorageException storageException = null;
    try {
      bucketInfo = storage.get(
          this.bucket, Storage.BucketGetOption.fields(Storage.BucketField.VERSIONING));
    } catch (StorageException ex) {
      storageException = ex;
    }

    assumeThat(
        "Storage exception",
        storageException,
        is(nullValue())
    );
    assumeThat(
        "Test bucket must exist",
        bucketInfo,
        not(nullValue())
    );


    assumeThat(
        "Object versioning must be enabled for test bucket",
        !versioned || bucketInfo.versioningEnabled(),
        is(true)
    );
  }


}
