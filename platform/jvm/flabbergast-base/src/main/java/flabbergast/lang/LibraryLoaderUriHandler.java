package flabbergast.lang;

import flabbergast.export.Library;
import flabbergast.export.LibraryLoader;
import flabbergast.export.LibraryResult;
import flabbergast.util.Result;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Load Flabbergast libraries */
final class LibraryLoaderUriHandler implements UriHandler {
  private static final ServiceLoader<LibraryLoader> LOADERS =
      ServiceLoader.load(LibraryLoader.class);

  private final ResourcePathFinder finder;
  private final Predicate<ServiceFlag> flags;

  public LibraryLoaderUriHandler(ResourcePathFinder finder, Predicate<ServiceFlag> flags) {
    this.finder = finder;
    this.flags = flags;
  }

  @Override
  public final String name() {
    return "Flabbergast libraries";
  }

  @Override
  public final int priority() {
    return 0;
  }

  @Override
  public Result<Promise<Any>> resolveUri(UriExecutor executor, URI uri) {
    return Result.of(uri)
        .filter(u -> u.getScheme().equals("lib"))
        .optionalMap(
            u ->
                Stream.concat(
                        LOADERS.stream().map(ServiceLoader.Provider::get),
                        Stream.of(
                            (finder, flags, libraryName) ->
                                finder
                                    .findProviders(Library.class)
                                    .map(ServiceLoader.Provider::get)
                                    .filter(pcl -> pcl.name().equals(libraryName))
                                    .map(
                                        pcl ->
                                            new LibraryResult() {
                                              @Override
                                              public int cost() {
                                                return 0;
                                              }

                                              @Override
                                              public Promise<Any> load(UriExecutor executor) {
                                                return executor.launch(pcl);
                                              }

                                              @Override
                                              public Instant timestamp() {
                                                return pcl.timestamp();
                                              }
                                            })))
                    .flatMap(ll -> ll.findAll(finder, flags, u.getSchemeSpecificPart()))
                    .max(
                        Comparator.comparing(LibraryResult::timestamp)
                            .thenComparing(Comparator.comparingInt(LibraryResult::cost).reversed()))
                    .map(lib -> lib.load(executor)));
  }
}
