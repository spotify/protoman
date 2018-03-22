package com.spotify.protoman.registry.storage;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.spotify.protoman.registry.SchemaVersion;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class ProtoIndexTest {

  @Test
  public void emptyProtoIndexShouldBeEmpty() {
    final ProtoIndex empty = ProtoIndex.empty();

    assertThat(empty.getPackageVersions().keySet(), hasSize(0));
    assertThat(empty.getProtoLocations().keySet(), hasSize(0));
  }

  @Test
  public void serdeShouldReturnSame() {
    final ProtoIndex empty = ProtoIndex.empty();
    final ProtoIndex parsed = ProtoIndex.parse(empty.toByteArray());

    assertThat(parsed.getPackageVersions(), equalTo(empty.getPackageVersions()));
    assertThat(parsed.getProtoLocations(), equalTo(empty.getProtoLocations()));
    assertThat(parsed.getProtoDependencies(), equalTo(empty.getProtoDependencies()));
  }

  @Test
  public void updateProtoLocation() {
    final ProtoIndex protoIndex = ProtoIndex.empty();
    protoIndex.updateProtoLocation("/pkg1/protofile.proto", "dest1");
    protoIndex.updateProtoLocation("/pkg2/protofile.proto", "dest2");

    final ProtoIndex after = ProtoIndex.parse(protoIndex.toByteArray());

    assertThat(after.getProtoLocations(), equalTo(ImmutableMap.of(
        "/pkg1/protofile.proto", "dest1",
        "/pkg2/protofile.proto", "dest2"
    )));
  }

  @Test
  public void removeProtoLocation() {
    final ProtoIndex protoIndex = ProtoIndex.empty();
    protoIndex.updateProtoLocation("/pkg1/protofile.proto", "dest1");
    protoIndex.removeProtoLocation("/pkg1/protofile.proto");

    final ProtoIndex after = ProtoIndex.parse(protoIndex.toByteArray());

    assertThat(after.getProtoLocations(), equalTo(ImmutableMap.of()));
  }

  @Test
  public void updatePackageVersion() {
    final ProtoIndex protoIndex = ProtoIndex.empty();
    protoIndex.updatePackageVersion("pkg1", SchemaVersion.create("1",0,0));

    final ProtoIndex after = ProtoIndex.parse(protoIndex.toByteArray());

    assertThat(after.getPackageVersions(), equalTo(ImmutableMap.of(
        "pkg1", SchemaVersion.create("1",0,0)
    )));
  }

  @Test
  public void updateProtoDependencies() {
    final Path path1 = Paths.get("/pkg1/proto1.proto");
    final Path path2 = Paths.get("/pkg2/proto2.proto");
    final Path path3 = Paths.get("/pkg3/proto3.proto");

    final ProtoIndex protoIndex = ProtoIndex.empty();
    protoIndex.updateProtoDependencies(path1, ImmutableSet.of(path1, path2));
    protoIndex.updateProtoDependencies(path2, ImmutableSet.of(path3));

    final ProtoIndex after = ProtoIndex.parse(protoIndex.toByteArray());

    assertThat(after.getProtoDependencies(), equalTo(ImmutableSetMultimap.of(
        path1, path1,
        path1, path2,
        path2, path3
    )));
  }

  @Test(expected = NullPointerException.class)
  public void updatePackageVersion_nullValueNotAllowed() {
    ProtoIndex.empty().updatePackageVersion("pkg1", null);
  }

  @Test(expected = NullPointerException.class)
  public void updatePackageVersion_nullKeyNotAllowed() {
    ProtoIndex.empty().updatePackageVersion(null,
        SchemaVersion.create("1",0,0));
  }

}