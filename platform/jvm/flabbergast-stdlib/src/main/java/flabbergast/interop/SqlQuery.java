package flabbergast.interop;

import flabbergast.export.LookupAssistant;
import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Performs a query on an SQL database and returns the result as a frame */
final class SqlQuery implements LookupAssistant.Recipient {

  private interface ArrayRetriever {
    void store(ResultSet rs, List<Attribute> builders) throws SQLException;
  }

  private interface ColumnRetriever {
    void store(ResultSet rs, List<Attribute> builders, AtomicReference<Name> attrName)
        throws SQLException;
  }

  private interface MakeRetriever {
    Stream<ColumnRetriever> make(int position, String columnName, int columnType);

    Stream<ArrayRetriever> make(int columnType);
  }

  private interface Retriever<T> {
    T access(ResultSet rs, int column) throws SQLException;
  }

  static final Definition DEFINITION =
      LookupAssistant.create(
          SqlQuery::new,
          LookupAssistant.find(
              AnyConverter.asProxy(Connection.class, false, SpecialLocation.uri("sql+")),
              (i, x) -> i.connection = x,
              "connection"),
          LookupAssistant.find(AnyConverter.asString(false), (i, x) -> i.query = x, "sql_query"));
  private static final List<MakeRetriever> MAKE_RETRIEVERS =
      List.of(
          makeName(
              (rs, position) -> Name.of(rs.getString(position)),
              Types.CHAR,
              Types.VARCHAR,
              Types.LONGVARCHAR),
          makeName(
              (rs, position) -> Name.of(rs.getLong(position)),
              Types.TINYINT,
              Types.SMALLINT,
              Types.INTEGER,
              Types.BIGINT),
          makeValue(
              (rs, position) -> Any.of(rs.getString(position)),
              Types.CHAR,
              Types.VARCHAR,
              Types.LONGVARCHAR),
          makeValue(
              withNull((rs, position) -> Any.of(rs.getLong(position))),
              Types.TINYINT,
              Types.SMALLINT,
              Types.INTEGER,
              Types.BIGINT),
          makeValue(
              withNull((rs, position) -> Any.of(rs.getDouble(position))),
              Types.REAL,
              Types.FLOAT,
              Types.DOUBLE,
              Types.NUMERIC,
              Types.DECIMAL),
          makeValue(
              withNull((rs, position) -> Any.of(rs.getBoolean(position))),
              Types.BIT,
              Types.BOOLEAN),
          makeCompute(
              (rs, position) ->
                  Frame.of(
                      ZonedDateTime.ofInstant(
                          rs.getTimestamp(
                                  position, Calendar.getInstance(TimeZone.getTimeZone("UTC")))
                              .toInstant(),
                          ZoneOffset.UTC)),
              Types.DATE,
              Types.TIME,
              Types.TIMESTAMP,
              Types.TIME_WITH_TIMEZONE,
              Types.TIMESTAMP_WITH_TIMEZONE),
          makeValue(
              (rs, position) -> {
                final var blob = rs.getBlob(position);
                return blob == null ? Any.NULL : Any.of(blob.getBytes(1, (int) blob.length()));
              },
              Types.BLOB),
          makeValue(
              (rs, position) -> {
                final var blob = rs.getClob(position);
                return blob == null ? Any.NULL : Any.of(blob.getSubString(1, (int) blob.length()));
              },
              Types.CLOB),
          makeValue(
              (rs, position) -> Any.of(rs.getNString(position)),
              Types.NCHAR,
              Types.NVARCHAR,
              Types.LONGNVARCHAR),
          makeValue(
              (rs, position) -> {
                final var blob = rs.getNClob(position);
                return blob == null ? Any.NULL : Any.of(blob.getSubString(1, (int) blob.length()));
              },
              Types.NCLOB),
          makeValue(
              (rs, position) -> Any.of(rs.getBytes(position)),
              Types.BINARY,
              Types.LONGVARBINARY,
              Types.VARBINARY),
          makeValue((rs, position) -> Any.of(rs.getRowId(position).toString()), Types.ROWID),
          makeValue((rs, position) -> Any.NULL, Types.NULL),
          makeCompute(
              (rs, column) -> {
                final var xml = rs.getSQLXML(column);
                if (xml == null) {
                  return Definition.constant(Any.NULL);
                }
                try (final var input = xml.getBinaryStream()) {
                  return NativeBinding.xml(input);
                } catch (Exception e) {
                  return Definition.error(e.getMessage());
                }
              },
              Types.SQLXML),
          makeCompute(
              (rs, column) -> {
                final var array = rs.getArray(column);
                if (array == null) {
                  return Definition.constant(Any.NULL);
                }
                final var baseType = array.getBaseType();
                final var retriever =
                    retrievers()
                        .flatMap(maker -> maker.make(baseType))
                        .findFirst()
                        .orElse(
                            (results, builder) ->
                                builder.add(
                                    Attribute.of(
                                        1,
                                        Definition.error(
                                            String.format(
                                                "Cannot convert SQL type “%s” inside array into Flabbergast type.",
                                                array.getBaseTypeName())))));

                final var items = new ArrayList<Attribute>();
                final var arrayResults = array.getResultSet();
                while (arrayResults.next()) {
                  retriever.store(arrayResults, items);
                }

                array.free();
                return Frame.define(AttributeSource.of(items));
              },
              Types.ARRAY),
          makeCompute(
              (rs, column) -> {
                final var innerRs = rs.getObject(column, ResultSet.class);
                return ((future, sourceReference, context) ->
                    () -> {
                      try {
                        processResultSet(future, sourceReference, context, innerRs);
                      } catch (SQLException e) {
                        future.error(sourceReference, e.getMessage());
                      }
                    });
              },
              Types.REF_CURSOR),
          makeCompute(
              (rs, column) -> {
                final var dataLink = rs.getURL(column);
                if (dataLink == null) {
                  return Definition.constant(Any.NULL);
                }
                return (f, s, c) ->
                    () -> {
                      try {
                        final var conn = dataLink.openConnection();
                        final var data = new byte[conn.getContentLength()];
                        try (final var inputStream = new DataInputStream(conn.getInputStream())) {
                          inputStream.readFully(data);
                          f.complete(Any.of(data));
                        }
                      } catch (IOException e) {
                        f.error(s, e.getMessage());
                      }
                    };
              },
              Types.DATALINK));

  private static MakeRetriever makeCompute(Retriever<Definition> retriever, int... sqlTypes) {
    return new MakeRetriever() {
      @Override
      public Stream<ColumnRetriever> make(int position, String columnName, int columnType) {
        return IntStream.of(sqlTypes).anyMatch(t -> t == columnType)
            ? Stream.of(
                (rs, builders, attrName) ->
                    builders.add(Attribute.of(columnName, retriever.access(rs, position))))
            : Stream.empty();
      }

      @Override
      public Stream<ArrayRetriever> make(int columnType) {
        return IntStream.of(sqlTypes).anyMatch(t -> t == columnType)
            ? Stream.of(
                (rs, builders) ->
                    builders.add(Attribute.of(rs.getLong(1), retriever.access(rs, 2))))
            : Stream.empty();
      }
    };
  }

  private static MakeRetriever makeName(Retriever<Name> retriever, int... sqlTypes) {
    return new MakeRetriever() {
      @Override
      public Stream<ColumnRetriever> make(int position, String columnName, int columnType) {
        return columnName.equals("") && IntStream.of(sqlTypes).anyMatch(t -> t == columnType)
            ? Stream.of((rs, builders, attrName) -> attrName.set(retriever.access(rs, position)))
            : Stream.empty();
      }

      @Override
      public Stream<ArrayRetriever> make(int columnType) {
        return Stream.empty();
      }
    };
  }

  private static MakeRetriever makeValue(Retriever<Any> retriever, int... sqlTypes) {
    return new MakeRetriever() {
      @Override
      public Stream<ColumnRetriever> make(int position, String columnName, int columnType) {
        return IntStream.of(sqlTypes).anyMatch(t -> t == columnType)
            ? Stream.of(
                (rs, builders, attrName) ->
                    builders.add(Attribute.of(columnName, retriever.access(rs, position))))
            : Stream.empty();
      }

      @Override
      public Stream<ArrayRetriever> make(int columnType) {
        return IntStream.of(sqlTypes).anyMatch(t -> t == columnType)
            ? Stream.of(
                (rs, builders) ->
                    builders.add(Attribute.of(rs.getLong(1), retriever.access(rs, 2))))
            : Stream.empty();
      }
    };
  }
  // Unsupported types: STRUCT, OTHER, JAVA_OBJECT, DISTINCT, REF

  private static void processResultSet(
      Future<Any> future, SourceReference sourceReference, Context context, ResultSet rs)
      throws SQLException {
    final var rsmd = rs.getMetaData();
    final var retrievers =
        IntStream.rangeClosed(1, rsmd.getColumnCount())
            .<ColumnRetriever>mapToObj(
                position -> {
                  try {
                    final var name = rsmd.getColumnLabel(position);
                    final var type = rsmd.getColumnType(position);
                    final var typeName = rsmd.getColumnTypeName(position);
                    return MAKE_RETRIEVERS
                        .stream()
                        .flatMap(maker -> maker.make(position, name, type))
                        .findFirst()
                        .orElse(
                            (results, builder, rowName) ->
                                builder.add(
                                    Attribute.of(
                                        name,
                                        Definition.error(
                                            String.format(
                                                "Cannot convert SQL type “%s” for column “%s” into Flabbergast type.",
                                                typeName, name)))));
                  } catch (final SQLException e) {
                    return (results, builder, rowName) ->
                        builder.add(Attribute.of(position, Definition.error(e.getMessage())));
                  }
                })
            .collect(Collectors.toList());

    final var rows = new ArrayList<Attribute>();
    for (var it = 1; rs.next(); it++) {
      final var builder = new ArrayList<Attribute>();
      final var name = new AtomicReference<>(Name.of(it));
      for (final var r : retrievers) {
        r.store(rs, builder, name);
      }
      rows.add(
          Attribute.of(
              name.get(),
              Template.instantiate(
                  AttributeSource.of(builder),
                  String.format("Row %d of SQL query results", it),
                  "sql_row_tmpl")));
    }
    future.complete(
        Any.of(Frame.create(future, sourceReference, context, AttributeSource.of(rows))));
  }

  public static Stream<MakeRetriever> retrievers() {
    return MAKE_RETRIEVERS.stream();
  }

  private static Retriever<Any> withNull(Retriever<Any> retriever) {
    return (rs, column) -> {
      final var result = retriever.access(rs, column);
      return rs.wasNull() ? Any.NULL : result;
    };
  }

  private Connection connection;
  private String query;

  public SqlQuery() {}

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    try (final var stmt = connection.createStatement();
        final var rs = stmt.executeQuery(query)) {
      processResultSet(future, sourceReference, context, rs);
    } catch (final Exception e) {
      future.error(sourceReference, e.getMessage());
    }
  }
}
