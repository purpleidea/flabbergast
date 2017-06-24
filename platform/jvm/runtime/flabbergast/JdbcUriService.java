package flabbergast;

import java.util.EnumSet;
import org.kohsuke.MetaInfServices;

@MetaInfServices
public class JdbcUriService implements UriService {
  @Override
  public UriHandler create(ResourcePathFinder finder, EnumSet<LoadRule> flags) {
    if (flags.contains(LoadRule.SANDBOXED)) {
      return null;
    }
    JdbcUriHandler handler = new JdbcUriHandler();
    handler.setFinder(finder);
    return handler;
  }
}
