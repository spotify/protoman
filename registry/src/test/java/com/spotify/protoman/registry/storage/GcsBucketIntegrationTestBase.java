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
