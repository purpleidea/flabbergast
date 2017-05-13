package flabbergast.interop;

import flabbergast.lang.ResourcePathFinder;
import flabbergast.util.Result;
import java.net.URI;
import java.net.URL;

/** Fetch files from FTP servers */
final class ServiceFtp extends UrlConnectionService {

  ServiceFtp() {
    super("FTP files", true);
  }

  @Override
  protected Result<URL> convert(URI uri, ResourcePathFinder finder) {
    return Result.of(uri)
        .filter(x -> x.getScheme().equals("ftp") || x.getScheme().equals("ftps"))
        .map(URI::toURL);
  }
}
