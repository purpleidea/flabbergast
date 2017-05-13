package flabbergast.compiler;

import java.nio.file.Path;
import java.util.stream.Stream;

public abstract class LanguageGrammar {
  public static LanguageGrammar plain() {}

  public static LanguageGrammar automatic() {
    return plain();
  }

  public static LanguageGrammar open(Path path) {
    throw new UnsupportedOperationException("Custom grammars are not yet supported");
  }

  public abstract Stream<String> keywords();
}
