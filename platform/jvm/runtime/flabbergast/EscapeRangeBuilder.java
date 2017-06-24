package flabbergast;

import flabbergast.Escape.Range;
import flabbergast.Escape.RangeAction;
import flabbergast.EscapeBuilder.Transformation;
import flabbergast.ReflectedFrame.Transform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EscapeRangeBuilder extends BaseReflectedInterop<Transformation> {
  private List<RangeAction> actions;
  private String end;
  private String start;

  public EscapeRangeBuilder(
      TaskMaster task_master,
      SourceReference source_reference,
      Context context,
      Frame self,
      Frame container) {
    super(task_master, source_reference, context, self, container);
  }

  @Override
  protected Transformation computeResult() throws Exception {
    int start_codepoint = EscapeBuilder.stringToCodepoint(start);
    int end_codepoint = EscapeBuilder.stringToCodepoint(end);
    if (start_codepoint > end_codepoint) {
      throw new IllegalArgumentException("Transformation range has start before end.");
    }
    Range range = new Range(start_codepoint, end_codepoint, actions);
    return builder -> builder.ranges.add(range);
  }

  @Override
  protected Map<String, Transform<Transformation>> getAccessors() {
    return Collections.emptyMap();
  }

  @Override
  protected void setup() {
    Sink<String> start_lookup = find(String.class, x -> start = x);
    start_lookup.allowDefault(false, null);
    start_lookup.lookup("start");
    Sink<String> end_lookup = find(String.class, x -> end = x);
    end_lookup.allowDefault(false, null);
    end_lookup.lookup("end");
    ListSink<RangeAction> actions_lookup =
        findAll(RangeAction.class, x -> actions = new ArrayList<>(x.values()));
    actions_lookup.allowDefault(false, "Frame is not one of the Unicode escapes known.");
    actions_lookup.allow(
        String.class, str -> (buffer, codepoint) -> buffer.append(str), false, null);
    actions_lookup.lookup("replacement");
  }
}
