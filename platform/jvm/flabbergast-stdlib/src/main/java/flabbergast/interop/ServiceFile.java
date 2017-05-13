package flabbergast.interop;

import flabbergast.lang.ResourcePathFinder;
import flabbergast.util.Result;
import java.net.URI;
import java.net.URL;

/** Allow reading local files */
class ServiceFile extends UrlConnectionService {

  public ServiceFile() {
    super("local files", true);
  }

  @Override
  protected Result<URL> convert(URI uri, ResourcePathFinder finder) {
    return Result.of(uri).filter(x -> x.getScheme().equals("file")).map(URI::toURL);
  }
}
