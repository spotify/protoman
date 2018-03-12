package com.spotify.protoman.registry.storage;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * NOTE:
 * This test requires a versioned GCS bucket called protoman-integration-tests to exist
 * and for versioning to be enabled on that bucket, or the test will be ignored
 */
public class GcsGenerationalFileIT extends GcsBucketIntegrationTestBase {

  private static final String BUCKET = "protoman-integration-tests";

  private static final byte[] INITIAL_CONTENT = "INITIAL".getBytes(Charsets.UTF_8);

  private GcsGenerationalFile index;
  private String filename;

  public GcsGenerationalFileIT() {
    super(StorageOptions.getDefaultInstance().getService(),
        BUCKET,
        true);
  }

  @Before
  public void setup() {
    filename = String.format("index-test-%d", Instant.now().toEpochMilli());

    index = GcsGenerationalFile.create(storage, bucket, filename);
    index.createIfNotExists(INITIAL_CONTENT);
  }

  @After
  public void destroy() {
    if (index != null) {
      storage.delete(BlobId.of(bucket, filename));
    }
  }

  @Test(expected = GcsGenerationalFile.NotFoundException.class)
  public void missingFileThrows() {

    final GcsGenerationalFile indexWithoutfile =
        GcsGenerationalFile.create(storage, bucket, "unknown-file");

    indexWithoutfile.load();
  }

  @Test()
  public void loadAndReplace() {
    byte[] content2 = "REPLACED".getBytes(Charsets.UTF_8);

    // load
    final byte[] loaded1 = index.load();
    final long generation1 = index.currentGeneration();

    // replace
    index.replace(content2);
    final byte[] loaded2 = index.load();
    final long generation2 = index.currentGeneration();

    assertThat(loaded1, is(INITIAL_CONTENT));
    assertTrue(generation1 < generation2);
    assertThat(loaded2, is(content2));
  }


  @Test(expected = GcsGenerationalFile.OptimisticLockingException.class)
  public void replaceShouldFailIfFileChanged() {
    // load
    index.load();

    {
      // load and replace from another instance
      final GcsGenerationalFile anotherFile =
          GcsGenerationalFile.create(storage, bucket, filename);

      anotherFile.load();
      anotherFile.replace("FOO".getBytes(Charsets.UTF_8));
    }

    // replace should fail
    index.replace("BAR".getBytes(Charsets.UTF_8));
  }

  @Test
  public void listGenerations() {
    index.load();

    final List<Long> gens = Lists.newArrayList();
    gens.add(index.currentGeneration());

    int NUM_UPDATES = 4;
    for (int i = 0; i < NUM_UPDATES; i++) {
      final long generation =
          index.replace(String.format("CONTENT-%d", i).getBytes(Charsets.UTF_8));
      gens.add(generation);
    }

    final List<Long> generationList = index.listGenerations().collect(Collectors.toList());
    assertThat(generationList, equalTo(gens));
  }

  @Test
  public void loadGeneration() {
    index.load();
    final long initialGeneration = index.currentGeneration();
    final byte[] newContent = "FOO".getBytes(Charsets.UTF_8);
    final long newGeneration = index.replace(newContent);

    assertThat(
        index.contentForGeneration(initialGeneration),
        equalTo(INITIAL_CONTENT)
    );

    assertThat(
        index.contentForGeneration(newGeneration),
        equalTo(newContent)
    );

    assertThat(
        index.contentForGeneration(index.currentGeneration()),
        equalTo(newContent)
    );

  }

}