package flabbergast.cli;

import flabbergast.lang.Context;
import flabbergast.lang.Frame;
import flabbergast.lang.Name;
import java.util.List;
import java.util.function.Supplier;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.Levenshtein;

class AttributeCompleter implements Completer {
  private final Supplier<Frame> source;

  public AttributeCompleter(Supplier<Frame> source) {
    this.source = source;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, final List<Candidate> candidates) {
    Context.EMPTY
        .forFrame(source.get())
        .names()
        .map(Name::toString)
        .filter(
            attribute ->
                attribute.regionMatches(true, 0, line.word(), 0, line.wordCursor())
                    || Levenshtein.distance(line.word(), attribute) < 5)
        .map(Candidate::new)
        .forEachOrdered(candidates::add);
  }
}
