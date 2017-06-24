package flabbergast;

import flabbergast.Escape.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class EscapeBuilder extends InterlockedLookup {
  public interface Transformation extends Consumer<EscapeBuilder> {}

  public static int stringToCodepoint(String str) {
    if (str.length() == 0) {
      throw new IllegalArgumentException("Empty string in transformation.");
    }
    if (str.offsetByCodePoints(0, 1) < str.length()) {
      throw new IllegalArgumentException(
          String.format("String “%s” must be a single character.", str));
    }
    return str.codePointAt(0);
  }

  final List<Range> ranges = new ArrayList<Range>();
  private final Frame self;
  final Map<Integer, String> single_substitutions = new TreeMap<Integer, String>();

  public EscapeBuilder(
      TaskMaster task_master,
      SourceReference source_reference,
      Context context,
      Frame self,
      Frame container) {
    super(task_master, source_reference, context);
    this.self = self;
  }

  @Override
  protected void resolve() {
    ranges.sort(null);
    Template tmpl =
        new Template(
            new BasicSourceReference(
                "Make escape template", "<escape>", 0, 0, 0, 0, source_reference),
            context,
            self);
    tmpl.set("value", Escape.create(single_substitutions, ranges));
    result = tmpl;
  }

  @Override
  protected void setup() {
    ListSink<Transformation> transformations_lookup =
        findAll(
            Transformation.class, input -> input.values().stream().forEach(x -> x.accept(this)));
    transformations_lookup.allowDefault(false, null);
    transformations_lookup.lookup("arg_values");
  }
}
