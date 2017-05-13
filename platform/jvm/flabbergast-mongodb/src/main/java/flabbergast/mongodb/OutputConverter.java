package flabbergast.mongodb;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.lang.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.bson.BsonRegularExpression;
import org.bson.BsonTimestamp;
import org.bson.BsonUndefined;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.Code;
import org.bson.types.CodeWithScope;
import org.bson.types.ObjectId;

interface OutputConverter {
  static Attribute convertObject(ChildConversionRule rule, Name name, Object value) {
    {
      if (value == null) {
        return Attribute.of(name, Any.NULL);
      }
      if (value instanceof Document) {
        return Attribute.of(name, define(rule.find(name), (Document) value));
      }
      if (value instanceof List) {
        return Attribute.of(
            name,
            Frame.define(
                ((List<?>) value)
                    .stream()
                    .map(
                        new Function<Object, Attribute>() {
                          private final OutputConverter converter = rule.find(name);
                          private long index;

                          @Override
                          public Attribute apply(Object o) {
                            return convertObject(x -> converter, Name.of(++index), o);
                          }
                        })
                    .collect(toSource())));
      }
      if (value instanceof Date) {
        return Attribute.of(
            name,
            Frame.of(ZonedDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault())));
      }
      if (value instanceof Boolean) {
        return Attribute.of(name, Any.of((Boolean) value));
      }
      if (value instanceof Double) {
        return Attribute.of(name, Any.of((Double) value));
      }
      if (value instanceof Integer) {
        return Attribute.of(name, Any.of((Integer) value));
      }
      if (value instanceof Long) {
        return Attribute.of(name, Any.of((Long) value));
      }
      if (value instanceof String) {
        return Attribute.of(name, Any.of((String) value));
      }
      if (value instanceof Binary) {
        return Attribute.of(name, Any.of(((Binary) value).getData()));
      }
      if (value instanceof BsonTimestamp) {
        return Attribute.of(
            name,
            Frame.of(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(((BsonTimestamp) value).getTime()),
                    ZoneId.systemDefault())));
      }
      if (value instanceof BsonRegularExpression) {
        return Attribute.of(
            name,
            Frame.proxyOf(
                (BsonRegularExpression) value,
                Stream.of(
                    ProxyAttribute.extractStr("pattern", BsonRegularExpression::getPattern))));
      }
      if (value instanceof ObjectId) {
        return Attribute.of(
            name,
            Frame.proxyOf(
                (ObjectId) value,
                Stream.of(
                    ProxyAttribute.extractInt(
                        "timestamp", (ObjectId x) -> (long) x.getTimestamp()))));
      }
      if (value instanceof BsonUndefined) {
        return Attribute.of(name, Any.NULL);
      }
      if (value instanceof CodeWithScope) {
        return Attribute.of(
            name,
            Template.function(
                AttributeSource.of(
                    Attribute.of("code", Any.of(((CodeWithScope) value).getCode())),
                    Attribute.of(
                        "scope",
                        define(OutputConverter::direct, ((CodeWithScope) value).getScope()))),
                "Instantiate JavaScript code function",
                "mongodb",
                "convert",
                "javascript"));
      }
      if (value instanceof Code) {
        return Attribute.of(
            name,
            Template.function(
                AttributeSource.of(
                    Attribute.of("code", Any.of(((Code) value).getCode())),
                    Attribute.of("scope", Any.NULL)),
                "Instantiate JavaScript code function",
                "mongodb",
                "convert",
                "javascript"));
      }
      return Attribute.of(
          name,
          Definition.error(
              String.format(
                  "Unable to convert value “%s” of Java type “%s” from MongoDB",
                  value, value.getClass())));
    }
  }

  static Definition define(OutputConverter outputConverter, Document document) {
    return (f, s, c) -> () -> f.complete(outputConverter.convert(f, s, c, document));
  }

  static Any direct(
      Future<?> future, SourceReference sourceReference, Context context, Document document) {
    return Any.of(
        Frame.create(
            future,
            sourceReference,
            context,
            document
                .entrySet()
                .stream()
                .map(
                    e ->
                        convertObject(
                            x -> OutputConverter::direct, Name.of(e.getKey()), e.getValue()))
                .collect(toSource())));
  }

  static OutputConverter frame(ChildConversionRule rule) {
    return (future, sourceReference, context, document) ->
        Any.of(
            Frame.create(
                future,
                sourceReference,
                context,
                document
                    .entrySet()
                    .stream()
                    .map(e -> convertObject(rule, Name.of(e.getKey()), e.getValue()))
                    .collect(toSource())));
  }

  static OutputConverter template(Template template, ChildConversionRule rule) {
    return (future, sourceReference, context, document) ->
        Any.of(
            Frame.create(
                future,
                sourceReference,
                context,
                template,
                document
                    .entrySet()
                    .stream()
                    .map(e -> convertObject(rule, Name.of(e.getKey()), e.getValue()))
                    .collect(toSource())));
  }

  Any convert(
      Future<?> future, SourceReference sourceReference, Context context, Document document);
}
