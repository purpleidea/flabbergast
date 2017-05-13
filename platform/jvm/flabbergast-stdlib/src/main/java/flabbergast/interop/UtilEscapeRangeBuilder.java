package flabbergast.interop;

import flabbergast.export.LookupAssistant;
import flabbergast.interop.UtilEscape.Range;
import flabbergast.interop.UtilEscape.RangeAction;
import flabbergast.lang.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Escape a range of characters */
class UtilEscapeRangeBuilder implements LookupAssistant.Recipient {

  private static ConversionOperation<? extends RangeAction> makeAction(String value) {
    return ConversionOperation.succeed(codepoint -> value);
  }

  static final Definition DEFINITION =
      LookupAssistant.create(
          UtilEscapeRangeBuilder::new,
          LookupAssistant.find(AnyConverter.asCodepoint(), (i, x) -> i.start = x, "start"),
          LookupAssistant.find(AnyConverter.asCodepoint(), (i, x) -> i.end = x, "end"),
          LookupAssistant.find(
              AnyConverter.frameOf(
                  AnyConverter.of(
                      AnyConverter.convertBool(value -> makeAction(value ? "True" : "False")),
                      AnyConverter.convertFloat(value -> makeAction(Double.toString(value))),
                      AnyConverter.convertProxyFrame(
                          RangeAction.class,
                          SpecialLocation.library("utils").attributes("str_transform").any()),
                      AnyConverter.convertInt(value -> makeAction(Long.toString(value))),
                      AnyConverter.convertStr(value -> makeAction(value.toString()))),
                  false),
              (i1, x1) -> i1.actions = new ArrayList<>(x1.values()),
              "replacement"));

  private List<RangeAction> actions;
  private int end;
  private int start;

  private UtilEscapeRangeBuilder() {}

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    if (start > end) {
      future.error(sourceReference, "Transformation range has start before end.");
    } else {
      final var range = new Range(start, end, actions);
      future.complete(
          Any.of(
              Frame.<EscapeTransformation>proxyOf(
                  sourceReference,
                  context,
                  (ranges, namedSubstitutions, singleSubstitutions) -> ranges.add(range),
                  Stream.empty())));
    }
  }
}
