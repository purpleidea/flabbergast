package flabbergast.mongodb;

import static flabbergast.export.NativeBinding.function;

import com.mongodb.Function;
import com.mongodb.client.*;
import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import flabbergast.util.Pair;
import flabbergast.util.Result;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.*;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

/** Access a MongoDB instance in Flabbergast */
public class MongoUriService implements UriService {
  static final AnyConverter<BsonValue> BSON_VALUE_ANY_CONVERTER =
      AnyConverter.of(
          AnyConverter.convertBool(b -> ConversionOperation.succeed(new BsonBoolean(b))),
          AnyConverter.convertBin(b -> ConversionOperation.succeed(new BsonBinary(b))),
          AnyConverter.convertFloat(f -> ConversionOperation.succeed(new BsonDouble(f))),
          AnyConverter.convertInt(i -> ConversionOperation.succeed(new BsonInt64(i))),
          AnyConverter.convertNull(() -> ConversionOperation.succeed(new BsonNull())),
          AnyConverter.convertStr(s -> ConversionOperation.succeed(new BsonString(s.toString()))),
          AnyConverter.convertFrame(
              f ->
                  ConversionOperation.extractProxy(
                      f, BsonValue.class, MongoUriService::convertArray)));
  static final AnyConverter<Bson> BSON_ANY_CONVERTER =
      AnyConverter.of(
          AnyConverter.convertFrame(
              f -> ConversionOperation.extractProxy(f, Bson.class, MongoUriService::convertFrame)));
  static final AnyConverter<BsonDocument> BSON_DOCUMENT_NULLABLE_ANY_CONVERTER =
      AnyConverter.of(
          AnyConverter.convertNull(() -> ConversionOperation.<BsonDocument>succeed(null)),
          AnyConverter.convertFrame(
              f ->
                  ConversionOperation.extractProxy(
                      f, BsonDocument.class, MongoUriService::convertFrame)));
  static final AnyConverter<MongoClient> CLIENT_ANY_CONVERTER =
      AnyConverter.asProxy(MongoClient.class, false, SpecialLocation.uri("mongodb"));
  static final AnyConverter<MongoDatabase> DATABASE_ANY_CONVERTER =
      AnyConverter.asProxy(MongoDatabase.class, false, SpecialLocation.uri("mongodb"));
  private static final UriHandler INSTANCE =
      new UriHandler() {
        @Override
        public String name() {
          return "MongoDB Endpoints";
        }

        @Override
        public int priority() {
          return 0;
        }

        @Override
        public Result<Promise<Any>> resolveUri(UriExecutor runner, URI uri) {
          if (!uri.getScheme().equals("mongodb")) {
            return Result.empty();
          }
          try {
            final var client = MongoClients.create(uri.toASCIIString());
            return Result.of(
                Any.of(
                    Frame.proxyOf(
                        "mongo_" + client.hashCode(),
                        uri.toString(),
                        client,
                        Stream.of(ProxyAttribute.fixed("uri", Any.of(uri.toString()))))));
          } catch (Exception e) {
            return Result.error(e.getMessage());
          }
        }
      };
  private static final UriHandler INTEROP =
      NativeBinding.create(
          "mongodb",
          NativeBinding.of(
              "list_databases",
              function(
                  NativeBinding.FRAME_BY_ATTRIBUTES,
                  client -> convert(Any::of, client.listDatabaseNames()),
                  CLIENT_ANY_CONVERTER,
                  "mongodb_connection")),
          NativeBinding.of(
              "get_database",
              function(
                  NativeBinding.asProxy(ProxyAttribute.extractStr("name", MongoDatabase::getName)),
                  MongoClient::getDatabase,
                  CLIENT_ANY_CONVERTER,
                  "mongodb_connection",
                  AnyConverter.asString(false),
                  "database")),
          NativeBinding.of(
              "list_collections",
              function(
                  NativeBinding.FRAME_BY_ATTRIBUTES,
                  (MongoDatabase db) -> convert(Any::of, db.listCollectionNames()),
                  DATABASE_ANY_CONVERTER,
                  "mongodb_database")),
          NativeBinding.of(
              "convert_datetime",
              function(
                  NativeBinding.asProxy(),
                  datetime -> new BsonDateTime(datetime.toInstant().toEpochMilli()),
                  AnyConverter.asDateTime(false),
                  "time")),
          NativeBinding.of(
              "convert_objectid",
              function(
                  NativeBinding.asProxy(),
                  BsonObjectId::new,
                  AnyConverter.of(
                      AnyConverter.convertProxyFrame(
                          ObjectId.class,
                          SpecialLocation.library("mongodb").attributes("query").any().invoked()),
                      AnyConverter.convertStr(
                          s ->
                              ObjectId.isValid(s.toString())
                                  ? ConversionOperation.succeed(new ObjectId(s.toString()))
                                  : ConversionOperation.<ObjectId>fail(
                                      "String is not valid object ID")),
                      AnyConverter.convertBin(
                          b ->
                              b.length == 12
                                  ? ConversionOperation.succeed(new ObjectId(b))
                                  : ConversionOperation.<ObjectId>fail(
                                      "Bin is not valid object ID"))),
                  "id")),
          NativeBinding.of(
              "convert_regex",
              function(
                  NativeBinding.asProxy(
                      ProxyAttribute.extractStr("pattern", BsonRegularExpression::getPattern)),
                  BsonRegularExpression::new,
                  AnyConverter.asString(false),
                  "regex",
                  AnyConverter.asString(true),
                  "options")),
          NativeBinding.of(
              "convert_code",
              function(
                  NativeBinding.asProxy(),
                  (js, scope) ->
                      scope == null
                          ? new BsonJavaScript(js)
                          : new BsonJavaScriptWithScope(js, scope),
                  AnyConverter.asString(false),
                  "code",
                  BSON_DOCUMENT_NULLABLE_ANY_CONVERTER,
                  "scope")),
          NativeBinding.of(
              "operation_limit",
              function(
                  NativeBinding.<FindTransformation>asProxy(),
                  limit -> i -> i.limit(limit.intValue()),
                  AnyConverter.asInt(false),
                  "limit")),
          NativeBinding.of(
              "operation_skip",
              function(
                  NativeBinding.<FindTransformation>asProxy(),
                  skip -> i -> i.limit(skip.intValue()),
                  AnyConverter.asInt(false),
                  "skip")),
          NativeBinding.of("operation_filter", makeOperation(FindIterable::filter)),
          NativeBinding.of("operation_max", makeOperation(FindIterable::max)),
          NativeBinding.of("operation_min", makeOperation(FindIterable::min)),
          NativeBinding.of("operation_projection", makeOperation(FindIterable::projection)),
          NativeBinding.of("operation_sort", makeOperation(FindIterable::sort)),
          NativeBinding.of("aggregate", AggregationOperation.DEFINITION),
          NativeBinding.of("find", FindOperation.DEFINITION),
          NativeBinding.of(
              "select_first",
              Any.of(
                  Frame.<ResultSelector>proxyOf(
                      "mongo_first",
                      "mongodb",
                      (f, s, c, i, r) -> r.convert(f, s, c, i.first()),
                      Stream.empty()))),
          NativeBinding.of(
              "select_all",
              Any.of(
                  Frame.<ResultSelector>proxyOf(
                      "mongo_all",
                      "mongodb",
                      (f, s, c, i, r) ->
                          Any.of(
                              Frame.create(
                                  f,
                                  s,
                                  c,
                                  AttributeSource.of(
                                      i.map(
                                              new Function<Document, Attribute>() {
                                                private long index;

                                                @Override
                                                public Attribute apply(Document document) {
                                                  return Attribute.of(
                                                      index++, OutputConverter.define(r, document));
                                                }
                                              })
                                          .into(new ArrayList<>())))),
                      Stream.empty()))),
          NativeBinding.of(
              "result_direct",
              Any.of(
                  Frame.<OutputConverter>proxyOf(
                      "mongodb_result_direct",
                      "mongodb",
                      OutputConverter::direct,
                      Stream.empty()))),
          NativeBinding.of(
              "result_template",
              function(
                  NativeBinding.asProxy(),
                  names ->
                      OutputConverter.frame(x -> names.getOrDefault(x, OutputConverter::direct)),
                  AnyConverter.frameOf(
                      AnyConverter.asProxy(
                          OutputConverter.class,
                          false,
                          SpecialLocation.library("mongodb").attributes("output").any()),
                      false),
                  "args")),
          NativeBinding.of(
              "result_template",
              function(
                  NativeBinding.asProxy(),
                  (template, names) ->
                      OutputConverter.template(
                          template, x -> names.getOrDefault(x, OutputConverter::direct)),
                  AnyConverter.asTemplate(false),
                  "template",
                  AnyConverter.frameOf(
                      AnyConverter.asProxy(
                          OutputConverter.class,
                          false,
                          SpecialLocation.library("mongodb").attributes("output").any()),
                      false),
                  "args")));

  static <T> AttributeSource convert(Function<T, Any> box, MongoIterable<T> iterable) {
    return AttributeSource.of(
        iterable
            .map(
                new Function<T, Attribute>() {
                  private long index;

                  @Override
                  public Attribute apply(T s) {
                    return Attribute.of(index++, box.apply(s));
                  }
                })
            .into(new ArrayList<>()));
  }

  private static ConversionOperation<? extends BsonValue> convertArray(Frame frame) {
    final var distribution = frame.nameTypes();
    if (distribution.first() == 0) {
      return ConversionOperation.frame(frame, BSON_VALUE_ANY_CONVERTER)
          .map(m -> new BsonArray(List.copyOf(m.values())));
    }
    return convertFrame(frame, distribution);
  }

  private static ConversionOperation<? extends BsonDocument> convertFrame(Frame frame) {
    return convertFrame(frame, frame.nameTypes());
  }

  private static ConversionOperation<? extends BsonDocument> convertFrame(
      Frame frame, Pair<Long, Long> distribution) {
    if (distribution.second() == 0) {
      return ConversionOperation.frame(frame, BSON_VALUE_ANY_CONVERTER)
          .map(
              attributes ->
                  new BsonDocument(
                      attributes
                          .entrySet()
                          .stream()
                          .map(
                              e ->
                                  new BsonElement(
                                      e.getKey()
                                          .apply(
                                              new NameFunction<String>() {
                                                @Override
                                                public String apply(long ordinal) {
                                                  return Long.toString(ordinal);
                                                }

                                                @Override
                                                public String apply(String name) {
                                                  return name;
                                                }
                                              }),
                                      e.getValue()))
                          .collect(Collectors.toList())));
    }
    return ConversionOperation.fail("Frame contains a mixture of string and ordinal names.");
  }

  private static Definition makeOperation(
      BiFunction<FindIterable<Document>, Bson, FindIterable<Document>> operation) {
    return function(
        NativeBinding.<FindTransformation>asProxy(),
        conditions -> i -> operation.apply(i, conditions),
        BSON_ANY_CONVERTER,
        "args");
  }

  @Override
  public Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    return flags.test(ServiceFlag.SANDBOXED) ? Stream.of(INTEROP) : Stream.of(INTEROP, INSTANCE);
  }
}
