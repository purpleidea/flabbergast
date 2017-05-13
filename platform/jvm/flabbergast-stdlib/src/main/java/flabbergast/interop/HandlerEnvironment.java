package flabbergast.interop;

import flabbergast.lang.*;
import flabbergast.util.Result;
import flabbergast.util.WhinyPredicate;
import java.net.URI;
import java.util.regex.Pattern;

/** Exports UNIX environment variables into Flabbergast */
final class HandlerEnvironment implements UriHandler {
  public static final HandlerEnvironment INSTANCE = new HandlerEnvironment();
  private static final WhinyPredicate<String> VALID_ENV =
      Pattern.compile("^[A-Z_][A-Z0-9_]*$").asMatchPredicate()::test;

  private HandlerEnvironment() {}

  @Override
  public String name() {
    return "Environment variables";
  }

  @Override
  public int priority() {
    return 0;
  }

  @Override
  public Result<Promise<Any>> resolveUri(UriExecutor runner, URI uri) {
    return Result.of(uri)
        .filter(x -> x.getScheme().equals("env"))
        .map(URI::getSchemeSpecificPart)
        .filter(VALID_ENV, "Environment variable does not follow POSIX naming.")
        .map(System::getenv)
        .map(Any::of);
  }
}
