package com.spotify.protoman.descriptor;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toMap;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.DescriptorProtos;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

@AutoValue
public abstract class DescriptorSet {

  public abstract ImmutableList<FileDescriptor> fileDescriptors();

  public static DescriptorSet create(final DescriptorProtos.FileDescriptorSet fds,
                                     final Predicate<Path> predicate) {
    final DescriptorPool pool = DescriptorPool.create();

    final ImmutableList<FileDescriptor> fileDescriptors =
        buildFileDescriptors(fds, pool, fdp -> predicate.test(Paths.get(fdp.getName())))
            .collect(ImmutableList.toImmutableList());

    return new AutoValue_DescriptorSet(fileDescriptors);
  }

  public static DescriptorSet empty() {
    return new AutoValue_DescriptorSet(ImmutableList.of());
  }

  /**
   * Co-traverse descriptors in two descriptor sets. The given visitor will be called with the
   * same descriptor from the respective descriptor sets, or if with only one of them when a
   * descriptor is present in only one of the descriptor sets.
   *
   * Descriptors are always matched by <b>name</b> between the descriptor sets, which might feel
   * non-intuitive for things like fields and enum values whose uniqueness is really determined by
   * number. The reason for matching by name is that preserving the name is important when using
   * FieldMask.
   */
  public static void compare(final ComparingVisitor visitor,
                             final DescriptorSet a,
                             final DescriptorSet b) {
    group(a, b, d -> d.fileDescriptors().stream())
        .forEach(grouping -> visitor.visit(grouping.a(), grouping.b()));
    group(a, b, d -> d.fileDescriptors().stream().flatMap(x -> x.messageTypes().stream()))
        .forEach(grouping -> compare(visitor, grouping.a(), grouping.b()));
    group(a, b, d -> d.fileDescriptors().stream().flatMap(x -> x.enumTypes().stream()))
        .forEach(grouping -> compare(visitor, grouping.a(), grouping.b()));
    group(a, b, d -> d.fileDescriptors().stream().flatMap(x -> x.services().stream()))
        .forEach(grouping -> compare(visitor, grouping.a(), grouping.b()));
  }

  private static void compare(final ComparingVisitor visitor,
                              @Nullable final MessageDescriptor a,
                              @Nullable final MessageDescriptor b) {
    visitor.visit(a, b);

    // Fields
    group(a, b, d -> d.fields().stream())
        .forEach(grouping -> visitor.visit(grouping.a(), grouping.b(), a, b));
    // Oneofs
    group(a, b, d -> d.oneofs().stream())
        .forEach(grouping -> visitor.visit(grouping.a(), grouping.b()));
    // Nested enums
    group(a, b, d -> d.enumTypes().stream())
        .forEach(grouping -> compare(visitor, grouping.a(), grouping.b()));
    // Nested messages
    group(a, b, d -> d.nestedTypes().stream())
        .forEach(grouping -> compare(visitor, grouping.a(), grouping.b()));
  }

  private static void compare(final ComparingVisitor visitor,
                              @Nullable final ServiceDescriptor a,
                              @Nullable final ServiceDescriptor b) {
    visitor.visit(a, b);

    group(a, b, d -> d.methods().stream())
        .forEach(grouping -> visitor.visit(grouping.a(), grouping.b()));
  }

  private static void compare(final ComparingVisitor visitor,
                              @Nullable final EnumDescriptor a,
                              @Nullable final EnumDescriptor b) {
    visitor.visit(a, b);

    // NOTE(staffan): We probably want to group values by number rather than by name, but
    // "option allow_alias = true;" f's this up. When enabled several values can have
    // the same number :(
    if ((a != null && a.options().hasAllowAlias()) || (b != null && b.options().hasAllowAlias())) {
      group(a, b, d -> d.values().stream(), EnumValueDescriptor::name)
          .forEach(grouping -> visitor.visit(grouping.a(), grouping.b()));
    } else {
      group(a, b, d -> d.values().stream(), EnumValueDescriptor::number)
          .forEach(grouping -> visitor.visit(grouping.a(), grouping.b()));
    }
  }

  /**
   * Given two descriptors, extract children of a specific type and group them by their identity.
   *
   * For example, extract all fields for a message and group them by their name. If a child present
   * in {@code a} is missing from {@code b} {@link Grouping#a()} will be set to {@code null}, and
   * vice versa.
   *
   * @param childMapper Function used to extract children of {@code a} and {@code b}.
   * @param keyMapper   Function used extract identifiers for children used to match which
   *                    object in {@code a} corresponds to the same child in {@code b} (if any).
   *                    For example, some types should be grouped by name and other by number
   *                    (enum values).
   */
  private static <Parent, Child, Key> Stream<Grouping<Child>> group(
      @Nullable final Parent a,
      @Nullable final Parent b,
      final Function<Parent, Stream<Child>> childMapper,
      final Function<Child, Key> keyMapper) {
    final Map<Key, Child> aMap =
        a == null ? Collections.emptyMap() : childMapper.apply(a).collect(toMap(keyMapper, d -> d));
    final Map<Key, Child> bMap =
        b == null ? Collections.emptyMap() : childMapper.apply(b).collect(toMap(keyMapper, d -> d));
    return Sets.union(aMap.keySet(), bMap.keySet())
        .stream()
        .map(key -> Grouping.create(aMap.get(key), bMap.get(key)));
  }

  /**
   * Group children by name.
   *
   * @see #group(Object, Object, Function, Function)
   */
  private static <Parent, Child extends GenericDescriptor> Stream<Grouping<Child>> group(
      @Nullable final Parent a,
      @Nullable final Parent b,
      final Function<Parent, Stream<Child>> childMapper) {
    return group(a, b, childMapper, GenericDescriptor::fullName);
  }


  /**
   * Takes a {@link com.google.protobuf.DescriptorProtos.FileDescriptorSet} and generates
   * higher level {@link com.google.protobuf.Descriptors.FileDescriptor} objects from it for
   * each {@link com.google.protobuf.DescriptorProtos.FileDescriptorProto} matching the supplied
   * predicate.
   */
  private static Stream<FileDescriptor> buildFileDescriptors(
      final DescriptorProtos.FileDescriptorSet fileDescriptorSet,
      final DescriptorPool pool,
      final Predicate<DescriptorProtos.FileDescriptorProto> includePredicate) {
    // Sanity check input - make sure all protos we depend on are included in the FDS
    final ImmutableSet<String> availableFiles = fileDescriptorSet.getFileList().stream()
        .map(DescriptorProtos.FileDescriptorProto::getName)
        .collect(ImmutableSet.toImmutableSet());
    fileDescriptorSet.getFileList().forEach(fileDescriptorProto -> {
      fileDescriptorProto.getDependencyList().forEach(dep -> {
        if (!availableFiles.contains(dep)) {
          throw new RuntimeException(
              String.format("Missing dependency. File %s depends on %s, which is not included in "
                            + "the descriptor set", fileDescriptorProto.getName(), dep)
          );
        }
      });
    });

    final ImmutableCollection<FileNode> dependencyGraph = buildDependencyGraph(fileDescriptorSet);

    // Determine dependencies of FileDescriptors matched by the given predicate. Save
    // references to matching descriptors and their dependencies.
    final Set<FileNode> processSet = new HashSet<>();
    bfs(
        dependencyGraph.stream().filter(node -> includePredicate.test(node.fileDescriptorProto)),
        node -> {
          processSet.add(node);
          // Traverse all dependencies
          return node.dependencies.stream();
        }
    );

    // Build FileDescriptor objects, starting with files with no dependents. Only FileDescriptors
    // for objects that were matched by the given predicate or aree dependencies thereof will are
    // built.
    bfs(
        // Start out at nodes that have no dependencies
        processSet.stream().filter(node -> node.dependencies.isEmpty()),
        node -> {
          final Stream<FileDescriptor> dependencies = node.dependencies.stream()
              .map(n -> checkNotNull(n.fileDescriptor));

          node.fileDescriptor = FileDescriptor.create(node.fileDescriptorProto, pool, dependencies);

          // Next visit all files that
          // - depend on this file
          // - has no unresolved dependencies
          // - should be processed
          return node.dependents.stream()
              .filter(depedent ->
                  processSet.contains(depedent)
                  && depedent.dependencies.stream().noneMatch(n -> n.fileDescriptor == null)
              );
        }
    );

    return dependencyGraph.stream()
        .filter(node -> includePredicate.test(node.fileDescriptorProto))
        .map(node -> checkNotNull(node.fileDescriptor));
  }

  private static ImmutableCollection<FileNode> buildDependencyGraph(
      final DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
    final ImmutableMap<String, FileNode> nodes = fileDescriptorSet.getFileList().stream()
        .collect(ImmutableMap.toImmutableMap(
            DescriptorProtos.FileDescriptorProto::getName,
            FileNode::new)
        );

    // Create edges
    nodes.forEach(
        (name, node) -> {
          final DescriptorProtos.FileDescriptorProto fdp = node.fileDescriptorProto;
          fdp.getDependencyList().forEach(
              depName -> {
                final FileNode depNode = nodes.get(depName);
                node.dependencies.add(depNode);
                depNode.dependents.add(node);
              });
        }
    );

    return nodes.values();
  }

  private static void bfs(final Stream<FileNode> startNodes,
                          final Function<FileNode, Stream<FileNode>> action) {
    final Queue<FileNode> q = new ArrayDeque<>();
    final Set<FileNode> queued = new HashSet<>();
    final Consumer<FileNode> maybeEnqueue = node -> {
      if (queued.add(node)) {
        q.add(node);
      }
    };

    startNodes.forEach(maybeEnqueue);
    while (!q.isEmpty()) {
      final FileNode node = q.poll();
      action.apply(node).forEach(maybeEnqueue);
    }
  }

  private static class FileNode {

    private final DescriptorProtos.FileDescriptorProto fileDescriptorProto;
    private FileDescriptor fileDescriptor = null;
    private List<FileNode> dependencies = new ArrayList<>();
    private List<FileNode> dependents = new ArrayList<>();

    private FileNode(final DescriptorProtos.FileDescriptorProto fileDescriptorProto) {
      this.fileDescriptorProto = fileDescriptorProto;
    }
  }

  @AutoValue
  static abstract class Grouping<T> {
    @Nullable abstract T a();
    @Nullable abstract T b();

    private static <T> Grouping<T> create(@Nullable final T a, @Nullable final T b) {
      return new AutoValue_DescriptorSet_Grouping<>(a, b);
    }
  }

  public interface ComparingVisitor {

    void visit(@Nullable FieldDescriptor a, @Nullable FieldDescriptor b,
               @Nullable MessageDescriptor bMessage, @Nullable MessageDescriptor aMessage);

    void visit(@Nullable MessageDescriptor a, @Nullable MessageDescriptor b);

    void visit(@Nullable EnumDescriptor a, @Nullable EnumDescriptor b);

    void visit(@Nullable EnumValueDescriptor a, @Nullable EnumValueDescriptor b);

    void visit(@Nullable ServiceDescriptor a, @Nullable ServiceDescriptor b);

    void visit(@Nullable MethodDescriptor a, @Nullable MethodDescriptor b);

    void visit(@Nullable OneofDescriptor a, @Nullable OneofDescriptor b);

    void visit(@Nullable FileDescriptor a, @Nullable FileDescriptor b);
  }
}
