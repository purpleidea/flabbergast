package flabbergast.coreos.etcd;

import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import flabbergast.util.Result;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.options.GetOption;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Access an etcd API endpoint in Flabbergast */
public final class EtcdUriService implements UriService {
  private static final Pattern AMPERSAND = Pattern.compile("&");
  static final AnyConverter<ByteSequence> BYTE_ANY_CONVERTER =
      AnyConverter.asBinOrStr(false, StandardCharsets.UTF_8).thenApply(ByteSequence::from);
  static final AnyConverter<ByteSequence> BYTE_ANY_CONVERTER_OR_NULL =
      AnyConverter.asBinOrStr(true, StandardCharsets.UTF_8)
          .thenApply(b -> b == null ? null : ByteSequence.from(b));
  private static final Pattern EQUALSIGN = Pattern.compile("=");
  static final AnyConverter<Client> ETCD_CONNECTION_ANY_CONVERTER =
      AnyConverter.asProxy(Client.class, false, SpecialLocation.uri("etcd"));
  private static final UriHandler INSTANCE =
      new UriHandler() {
        @Override
        public String name() {
          return "etcd Endpoints";
        }

        @Override
        public int priority() {
          return 0;
        }

        @Override
        public Result<Promise<Any>> resolveUri(UriExecutor runner, URI uri) {
          if (!uri.getScheme().equals("etcd")) {
            return Result.empty();
          }
          try {

            final var endpoint =
                new URI("http", null, uri.getHost(), uri.getPort(), "", null, null);
            String authority = null;
            String user = null;
            String password = null;

            for (final var paramChunk : AMPERSAND.split(uri.getQuery())) {
              final var parts = EQUALSIGN.split(paramChunk, 2);
              if (parts.length != 2) {
                return Result.error(String.format("Invalid URL parameter “%s”.", paramChunk));
              }
              switch (parts[0]) {
                case "authority":
                  authority = parts[1];
                  break;
                case "user":
                  user = parts[1];
                  break;
                case "password":
                  password = parts[1];
                  break;
                default:
                  return Result.error(String.format("Unknown URL parameter “%s”.", parts[0]));
              }
            }
            if (authority == null || user == null || password == null) {
              return Result.error("All of “authority”, “user”, “password” must be specified.");
            }
            final var client =
                Client.builder()
                    .endpoints(endpoint.toString())
                    .authority(authority)
                    .user(ByteSequence.from(user, StandardCharsets.UTF_8))
                    .password(ByteSequence.from(password, StandardCharsets.UTF_8))
                    .build();
            return Result.of(
                Any.of(
                    Frame.proxyOf(
                        "etc_" + client.hashCode(),
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
          "coreos/etcd",
          Stream.of(
                  Stream.of(NativeBinding.of("get", GetKV.DEFINITION)),
                  Stream.of(NativeBinding.of("alarms", GetAlarms.DEFINITION)),
                  Stream.of(NativeBinding.of("members", GetMembers.DEFINITION)),
                  Stream.of(GetOption.SortTarget.values())
                      .map(
                          target ->
                              NativeBinding.of(
                                  "sort.field." + target.name().toLowerCase(),
                                  Any.of(
                                      Frame.proxyOf(
                                          "etc_sort_by_" + target.name().toLowerCase(),
                                          "etcd",
                                          target,
                                          Stream.of(
                                              ProxyAttribute.extractStr(
                                                  "order_by", GetOption.SortTarget::name)))))),
                  Stream.of(GetOption.SortOrder.values())
                      .map(
                          order ->
                              NativeBinding.of(
                                  "ordering." + order.name().toLowerCase(),
                                  Any.of(
                                      Frame.proxyOf(
                                          "etc_sort_" + order.name().toLowerCase(),
                                          "etcd",
                                          order,
                                          Stream.of(
                                              ProxyAttribute.extractStr(
                                                  "order", GetOption.SortOrder::name)))))))
              .flatMap(Function.identity()));

  @Override
  public Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    return flags.test(ServiceFlag.SANDBOXED) ? Stream.of(INTEROP) : Stream.of(INTEROP, INSTANCE);
  }
}
