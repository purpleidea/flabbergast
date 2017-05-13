package flabbergast.compiler;

import flabbergast.util.Pair;
import java.io.PrintStream;
import java.util.stream.Stream;

public interface ErrorCollector {
  static ErrorCollector of(PrintStream stream) {
    return new ErrorCollector() {

      @Override
      public void emitError(SourceLocation where, String error) {
        stream.printf(
            "%s:%d:%d-%d:%d: %s\n",
            where.getFileName(),
            where.getStartLine(),
            where.getStartColumn(),
            where.getEndLine(),
            where.getEndColumn(),
            error);
      }

      @Override
      public void emitConflict(String error, Stream<Pair<SourceLocation, String>> locations) {
        stream.println(error);
        locations.forEach(
            pair ->
                stream.printf(
                    "%s:%d:%d-%d:%d: %s\n",
                    pair.first().getFileName(),
                    pair.first().getStartLine(),
                    pair.first().getStartColumn(),
                    pair.first().getEndLine(),
                    pair.first().getEndColumn(),
                    pair.second()));
      }
    };
  }

  static ErrorCollector toStandardError() {
    return of(System.err);
  }

  void emitError(SourceLocation location, String error);

  void emitConflict(String error, Stream<Pair<SourceLocation, String>> locations);
}
