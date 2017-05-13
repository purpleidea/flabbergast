package flabbergast.facebook.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

final class PerformQuery implements LookupAssistant.Recipient {
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
  private static final ObjectMapper MAPPER = new ObjectMapper();
  static final NameFunction<String> TO_STR =
      new NameFunction<>() {
        @Override
        public String apply(long ordinal) {
          return Long.toString(ordinal);
        }

        @Override
        public String apply(String name) {
          return name;
        }
      };
  Client client;
  String operationName;
  String query;
  Map<Name, Output> responseType;
  Map<Name, Input> variables;

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    try {
      final var requestBody = MAPPER.createObjectNode();
      requestBody.put("query", query);
      requestBody.put("operationName", operationName);
      final var arguments = requestBody.putObject("variables");
      for (final var input : variables.entrySet()) {
        input.getValue().write(arguments, input.getKey().apply(TO_STR));
      }
      final var request =
          HttpRequest.newBuilder()
              .uri(client.uri())
              .POST(HttpRequest.BodyPublishers.ofByteArray(MAPPER.writeValueAsBytes(requestBody)))
              .build();
      HTTP_CLIENT
          .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
          .thenAccept(
              response -> {
                try {
                  if (response.statusCode() / 100 != 2) {
                    future.error(
                        sourceReference,
                        String.format("GraphQL HTTP error %d", response.statusCode()));
                    return;
                  }
                  final var responseJson = MAPPER.readTree(response.body());
                  if (responseJson.has("errors")) {
                    for (final var error : responseJson.get("errors")) {
                      future.error(
                          sourceReference, error.asText("Invalid error in response (client-side)"));
                    }
                  } else if (responseJson.has("data")) {
                    final var reader = responseJson.get("data");
                    if (reader.isObject()) {
                      final var attributes =
                          responseType
                              .entrySet()
                              .stream()
                              .map(
                                  arg ->
                                      arg.getValue()
                                          .read(reader.get(arg.getKey().toString()), arg.getKey()))
                              .collect(AttributeSource.toSource());
                      future.complete(
                          Any.of(Frame.create(future, sourceReference, context, attributes)));
                    } else {
                      future.error(
                          sourceReference,
                          String.format(
                              "GraphQL serer returned invalid data. Root data node is %s but should be object.",
                              reader.getNodeType().name()));
                    }
                  } else {
                    future.error(
                        sourceReference, "GraphQL response does not contain data nor errors.");
                  }
                } catch (Exception e) {
                  future.error(sourceReference, e.getMessage());
                }
              });
    } catch (Exception e) {
      future.error(sourceReference, e.getMessage());
    }
  }
}
