package flabbergast.facebook.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import flabbergast.lang.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

interface Output {
  static Any as(
      String typeName,
      boolean nullable,
      Predicate<JsonNode> check,
      Function<JsonNode, Any> converter) {
    return Any.of(
        Frame.<Output>proxyOf(
            "facebook_graphql_" + typeName + (nullable ? "_or_null" : ""),
            "facebook/graphql",
            (input, name) -> {
              if (check.test(input)) {
                return Attribute.of(name, converter.apply(input));
              } else if (nullable && input.isNull()) {
                return Attribute.of(name, Any.NULL);
              } else {
                return Attribute.of(
                    name,
                    Definition.error(
                        String.format(
                            "GraphQL provided %s when %s%s was expected.",
                            input.getNodeType().name(), typeName, nullable ? " or Null" : "")));
              }
            },
            Stream.empty()));
  }

  static Output asList(Output inner, boolean nullable) {
    if (inner.isList()) {
      throw new IllegalArgumentException("Cannot create a list of lists in GraphQL.");
    }
    return new Output() {
      @Override
      public boolean isList() {
        return true;
      }

      @Override
      public Attribute read(JsonNode reader, Name name) {
        if (reader.isArray()) {
          final var attributes = new ArrayList<Attribute>();
          var position = 0;
          for (final var element : reader) {
            inner.read(element, Name.of(++position));
          }
          return Attribute.of(name, Frame.define(AttributeSource.of(attributes)));
        } else if (nullable && reader.isNull()) {
          return Attribute.of(name, Any.NULL);
        } else {
          return Attribute.of(
              name,
              Definition.error(
                  String.format(
                      "GraphQL provided %s when array%s was expected.",
                      reader.getNodeType().name(), nullable ? " or Null" : "")));
        }
      }
    };
  }

  static Output asObject(Map<Name, Output> args, boolean nullable, Template template) {
    return (reader, name) -> {
      if (reader.isObject()) {
        final var attributes =
            args.entrySet()
                .stream()
                .map(arg -> arg.getValue().read(reader.get(arg.getKey().toString()), arg.getKey()))
                .collect(AttributeSource.toSource());
        return Attribute.of(
            name,
            template == null
                ? Frame.define(attributes)
                : template.instantiateWith(
                    "instantiate template for GraphQL object result", attributes));
      } else if (nullable && reader.isNull()) {
        return Attribute.of(name, Any.NULL);
      } else {
        return Attribute.of(
            name,
            Definition.error(
                String.format(
                    "GraphQL provided %s when object%s was expected.",
                    reader.getNodeType().name(), nullable ? " or Null" : "")));
      }
    };
  }

  default boolean isList() {
    return false;
  }

  Attribute read(JsonNode input, Name name);
}
