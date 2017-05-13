package flabbergast.interop;

import flabbergast.lang.ResourcePathFinder;
import flabbergast.util.Result;
import java.io.File;
import java.net.URI;
import java.net.URL;

/** Load files from resource directories */
final class ServiceResource extends UrlConnectionService {
  ServiceResource() {
    super("resource files", false);
  }

  @Override
  protected Result<URL> convert(URI uri, ResourcePathFinder finder) {

    if (!uri.getScheme().equals("res")) {
      return Result.empty();
    }
    return Result.of(uri)
        .map(URI::getSchemeSpecificPart)
        .optionalMap(tail -> finder.find(tail, ""))
        .map(File::toURI)
        .map(URI::toURL);
  }
}
