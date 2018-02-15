package com.spotify.protoman.descriptor;

import com.google.protobuf.DescriptorProtos;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

class PathNode {

  private static final PathNode DUMMY_NODE = new PathNode(null);

  private final PathNode parent;
  private final Map<Integer, PathNode> children = new HashMap<>();
  @Nullable private DescriptorProtos.SourceCodeInfo.Location location;

  static PathNode buildPathTree(final DescriptorProtos.SourceCodeInfo sourceCodeInfo) {
    final PathNode root = new PathNode(null);
    sourceCodeInfo.getLocationList().forEach(
        location -> {
          PathNode p = root;
          for (final int segment : location.getPathList()) {
            p = p.addChild(segment);
          }
          p.setLocation(location);
        }
    );
    return root;
  }

  static PathNode emptyTree() {
    return DUMMY_NODE;
  }

  private PathNode(final PathNode parent) {
    this.parent = parent;
    this.location = null;
  }

  private PathNode get(final int tag) {
    final PathNode ret = children.get(tag);
    return ret != null ? ret : DUMMY_NODE;
  }

  private PathNode addChild(final int tag) {
    if (children.containsKey(tag)) {
      return children.get(tag);
    }
    final PathNode pathNode = new PathNode(this);
    children.put(tag, pathNode);
    return pathNode;
  }

  private PathNode index(final int index) {
    return get(index);
  }

  private void setLocation(final DescriptorProtos.SourceCodeInfo.Location location) {
    this.location = location;
  }

  Optional<DescriptorProtos.SourceCodeInfo.Location> location() {
    return Optional.ofNullable(location);
  }

  Optional<SourceCodeInfo> sourceCodeInfo(final FileDescriptor file) {
    return location().map(location -> SourceCodeInfo.create(Paths.get(file.name()), location));
  }

  PathNode messageType(final int index) {
    return get(parent == null ? 4 : 3).index(index);
  }

  PathNode field(final int index) {
    return get(2).index(index);
  }

  PathNode enumType(final int index) {
    return get(parent == null ? 5 : 4).index(index);
  }

  PathNode enumValue(final int index) {
    return get(2).index(index);
  }

  PathNode service(final int index) {
    return get(6).index(index);
  }

  PathNode method(final int index) {
    return get(2).index(index);
  }

  PathNode oneof(final int index) {
    return get(8).index(index);
  }
}