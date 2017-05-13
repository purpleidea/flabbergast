package flabbergast.facebook.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import flabbergast.export.LookupAssistant;
import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import flabbergast.util.Result;
import java.net.URI;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Access a GraphQL API endpoint in Flabbergast */
public final class GraphQLUriService implements UriService {
  public static final AnyConverter<Output> CONVERTER_FOR_OUTPUT =
      AnyConverter.asProxy(
          Output.class,
          false,
          SpecialLocation.library("facebook", "graphql").attributes("response"));
  private static final UriHandler INSTANCE =
      new UriHandler() {
        @Override
        public String name() {
          return "GraphQL Endpoints";
        }

        @Override
        public int priority() {
          return 0;
        }

        @Override
        public Result<Promise<Any>> resolveUri(UriExecutor runner, URI uri) {
          if (!uri.getScheme().equals("graphql")) {
            return Result.empty();
          }
          final var client = new Client("http:" + uri.getPath());

          return Result.of(
              Any.of(
                  Frame.proxyOf(
                      "graphql_" + client.hashCode(),
                      uri.toString(),
                      client,
                      Stream.of(ProxyAttribute.fixed("uri", Any.of(uri.toString()))))));
        }
      };
  private static final UriHandler INTEROP =
      NativeBinding.create(
          "facebook/graphql",
          NativeBinding.of(
              "response.boolean",
              Output.as("Boolean", false, JsonNode::isBoolean, x -> Any.of(x.asBoolean()))),
          NativeBinding.of(
              "response.float",
              Output.as(
                  "Float", false, JsonNode::isFloatingPointNumber, x -> Any.of(x.asDouble()))),
          NativeBinding.of(
              "response.int",
              Output.as("Int", false, JsonNode::isIntegralNumber, x -> Any.of(x.asLong()))),
          NativeBinding.of(
              "response.string",
              Output.as("Str", false, JsonNode::isTextual, x -> Any.of(x.asText()))),
          NativeBinding.of(
              "response.boolean_or_null",
              Output.as("Boolean", true, JsonNode::isBoolean, x -> Any.of(x.asBoolean()))),
          NativeBinding.of(
              "response.float_or_null",
              Output.as("Float", true, JsonNode::isFloatingPointNumber, x -> Any.of(x.asDouble()))),
          NativeBinding.of(
              "response.int_or_null",
              Output.as("Int", true, JsonNode::isIntegralNumber, x -> Any.of(x.asLong()))),
          NativeBinding.of(
              "response.string_or_null",
              Output.as("Str", true, JsonNode::isTextual, x -> Any.of(x.asText()))),
          NativeBinding.of(
              "response.list",
              NativeBinding.function(
                  NativeBinding.asProxy(),
                  Output::asList,
                  CONVERTER_FOR_OUTPUT,
                  "of",
                  AnyConverter.asBool(false),
                  "nullable")),
          NativeBinding.of(
              "response.object",
              NativeBinding.function(
                  NativeBinding.asProxy(),
                  Output::asObject,
                  AnyConverter.frameOf(CONVERTER_FOR_OUTPUT, false),
                  "args",
                  AnyConverter.asBool(false),
                  "nullable",
                  AnyConverter.asTemplate(true),
                  "template")),
          NativeBinding.of(
              "query",
              LookupAssistant.create(
                  PerformQuery::new,
                  LookupAssistant.find(
                      AnyConverter.asProxy(Client.class, false, SpecialLocation.uri("graphql")),
                      (vars, c) -> vars.client = c,
                      "graphql_connection"),
                  LookupAssistant.find(
                      AnyConverter.asString(false), (vars, q) -> vars.query = q, "query"),
                  LookupAssistant.find(
                      AnyConverter.asString(false),
                      (vars, name) -> vars.operationName = name,
                      "operation_name"),
                  LookupAssistant.find(
                      AnyConverter.frameOf(InputConverter.OBJECT_INPUT_CONVERTER, false),
                      (vars, v) -> vars.variables = v,
                      "args"),
                  LookupAssistant.find(
                      AnyConverter.frameOf(CONVERTER_FOR_OUTPUT, false),
                      (vars, r) -> vars.responseType = r,
                      "response"))));

  @Override
  public Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    return flags.test(ServiceFlag.SANDBOXED) ? Stream.of(INTEROP) : Stream.of(INTEROP, INSTANCE);
  }
}
