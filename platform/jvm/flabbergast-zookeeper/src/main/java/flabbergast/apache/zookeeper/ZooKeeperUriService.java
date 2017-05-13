package flabbergast.apache.zookeeper;

import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import flabbergast.util.Result;
import java.net.URI;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.zookeeper.Version;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.common.PathUtils;

public final class ZooKeeperUriService implements UriService {
  private static final UriHandler HANDLER =
      new UriHandler() {
        @Override
        public int priority() {
          return 0;
        }

        @Override
        public String name() {
          return "ZooKeeper gateway";
        }

        @Override
        public Result<Promise<Any>> resolveUri(UriExecutor executor, URI uri) {
          return Result.of(uri)
              .filter(x -> x.getScheme().equals("zookeeper"))
              .map(
                  x ->
                      Frame.proxyOf(
                          "zk" + uri.hashCode(),
                          uri.toString(),
                          new ZooKeeper(x.getSchemeSpecificPart(), 2000, null),
                          Stream.of(
                              ProxyAttribute.fixed("uri", Any.of(uri.toString())),
                              ProxyAttribute.extractInt("session_id", ZooKeeper::getSessionId))))
              .map(Any::of);
        }
      };
  private static final UriHandler INTEROP =
      NativeBinding.create(
          "apache/zookeeper",
          NativeBinding.of("get", GetData.DEFINITION),
          NativeBinding.of("version", Any.of(Version.getVersion())),
          NativeBinding.of(
              "validate_path",
              NativeBinding.mapFunction(
                  NativeBinding.BOOL,
                  AnyConverter.asString(false),
                  (path, isSequential) -> {
                    try {
                      PathUtils.validatePath(path, isSequential);
                      return true;
                    } catch (IllegalArgumentException e) {
                      return false;
                    }
                  },
                  AnyConverter.asBool(false),
                  "is_sequential")));

  @Override
  public Stream<UriHandler> create(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    return flags.test(ServiceFlag.SANDBOXED) ? Stream.of(INTEROP) : Stream.of(INTEROP, HANDLER);
  }
}
