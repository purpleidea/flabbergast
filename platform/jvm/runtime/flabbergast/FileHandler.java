package flabbergast;

import java.net.URL;

public class FileHandler extends UrlConnectionHandler {

  public static final FileHandler INSTANCE = new FileHandler();

  @Override
  protected URL convert(String uri) throws Exception {

    if (!uri.startsWith("file:")) return null;
    return new URL(uri);
  }

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public String getUriName() {
    return "local files";
  }
}
