package com.spotify.protoman.registry;

import com.spotify.protoman.registry.storage.jooq.Tables;
import com.spotify.protoman.registry.storage.jooq.tables.records.FileRecord;
import com.spotify.protoman.registry.storage.jooq.tables.records.PackageVersionRecord;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public class SqlSchemaStorage implements SchemaStorage {

  private final ConnectionFactory connectionFactory;

  private SqlSchemaStorage(final ConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  public static SqlSchemaStorage create(final ConnectionFactory connectionFactory) {
    return new SqlSchemaStorage(connectionFactory);
  }

  @Override
  public Transaction open() {
    final Connection connection;
    try {
      connection = connectionFactory.getConnection();
      connection.setAutoCommit(false);
    } catch (SQLException e) {
      // TODO(staffan): do proper checked exception
      throw new IllegalStateException("Could not open connection", e);
    }
    return new TransactionImpl(connection);
  }

  private static class TransactionImpl implements SchemaStorage.Transaction {

    private final Connection connection;
    private final DSLContext ctx;

    public TransactionImpl(final Connection connection) {
      this.connection = connection;
       this.ctx = DSL.using(connection, SQLDialect.POSTGRES);
    }

    @Override
    public void storeFile(final SchemaFile file) {
      final FileRecord record = new FileRecord();
      record.setPath(file.path().toString());
      record.setContent(file.content());

      final Optional<Integer> existingId =
          ctx.select(Tables.FILE.ID)
              .from(Tables.FILE)
              .where(Tables.FILE.PATH.eq(file.path().toString()))
              .fetchOptional()
              .map(Record1::value1);

      if (existingId.isPresent()) {
        ctx.update(Tables.FILE).set(record).where(Tables.FILE.ID.eq(existingId.get())).execute();
      } else {
        ctx.insertInto(Tables.FILE).set(record).execute();
      }
    }

    @Override
    public Stream<SchemaFile> fetchAllFiles() {
      return ctx.selectFrom(Tables.FILE)
          .stream()
          .map(fileRecord ->
              SchemaFile.create(Paths.get(fileRecord.getPath()), fileRecord.getContent())
          );
    }

    @Override
    public void storePackageVersion(final String protoPackage, final SchemaVersion version) {
      final PackageVersionRecord record = new PackageVersionRecord();
      record.setPackage(protoPackage);
      record.setMajor(version.major());
      record.setMinor(version.minor());
      record.setPatch(version.patch());

      final Optional<Integer> existingId =
          ctx.select(Tables.PACKAGE_VERSION.ID)
              .from(Tables.PACKAGE_VERSION)
              .where(Tables.PACKAGE_VERSION.PACKAGE.eq(protoPackage))
              .fetchOptional()
              .map(Record1::value1);

      if (existingId.isPresent()) {
        ctx.update(Tables.PACKAGE_VERSION)
            .set(record)
            .where(Tables.PACKAGE_VERSION.ID.eq(existingId.get()))
            .execute();
      } else {
        ctx.insertInto(Tables.PACKAGE_VERSION)
            .set(record)
            .execute();
      }
    }

    @Override
    public Optional<SchemaVersion> getPackageVersion(final String protoPackage) {
      return ctx.select(
          Tables.PACKAGE_VERSION.MAJOR, Tables.PACKAGE_VERSION.MINOR, Tables.PACKAGE_VERSION.PATCH)
          .from(Tables.PACKAGE_VERSION)
          .where(Tables.PACKAGE_VERSION.PACKAGE.eq(protoPackage))
          .fetchOptional()
          .map(record -> SchemaVersion.create(record.value1(), record.value2(), record.value3()));
    }

    @Override
    public void commit() {
      try {
        connection.commit();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void close() {
      try {
        connection.close();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @FunctionalInterface
  public interface ConnectionFactory {

    Connection getConnection() throws SQLException;
  }
}
