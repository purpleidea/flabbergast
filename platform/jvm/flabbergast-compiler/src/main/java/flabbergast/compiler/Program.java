package flabbergast.compiler;

import flabbergast.compiler.kws.KwsRenderer;
import flabbergast.export.LibraryLoader;
import java.util.Optional;
import java.util.stream.Stream;
import org.w3c.dom.Document;

public final class Program {
  public static Program compile(
      LanguageGrammar grammar, String source, String filename, ErrorCollector errorCollector) {
    // TODO

  }

  public static LibraryLoader provider() {
    return (finder, flags, libraryName) -> {
      return Stream.empty(); // TODO
    };
  }

  public Program splice(int start, int end, String replacement, ErrorCollector errorCollector) {}

  public Document generateApi() {}

  public Stream<SyntaxHighlight> highlighting() {}

  public Optional<KwsRenderer> finish() {
    // TODO
  }
}
