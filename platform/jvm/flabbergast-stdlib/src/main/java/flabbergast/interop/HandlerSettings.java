package flabbergast.interop;

import flabbergast.lang.*;
import flabbergast.util.Result;
import java.net.URI;

/** Export JVM-specific settings to Flabbergast */
class HandlerSettings implements UriHandler {

  static final HandlerSettings INSTANCE = new HandlerSettings();

  private HandlerSettings() {}

  @Override
  public String name() {
    return "VM-specific settings";
  }

  @Override
  public int priority() {
    return 0;
  }

  @Override
  public Result<Promise<Any>> resolveUri(UriExecutor runner, URI uri) {
    if (!uri.getScheme().equals("settings")) {
      return null;
    }
    return Result.of(uri.getSchemeSpecificPart()).map(System::getProperty).map(Any::of);
  }
}
