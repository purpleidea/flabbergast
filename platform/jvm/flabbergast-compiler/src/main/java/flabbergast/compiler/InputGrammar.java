package flabbergast.compiler;

import java.io.OutputStream;
import java.util.stream.Stream;

public final class InputGrammar {
  public static InputGrammar parse(String source, ErrorCollector errorCollector) {}

  public InputGrammar splice(
      int offset, int length, String replacement, ErrorCollector errorCollector) {}

  public boolean isValid() {}

  public static void buildGrammar(Stream<InputGrammar> grammars, OutputStream output) {}
}
