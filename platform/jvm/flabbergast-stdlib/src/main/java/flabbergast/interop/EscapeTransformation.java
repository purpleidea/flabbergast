package flabbergast.interop;

import java.util.List;
import java.util.Map;

interface EscapeTransformation {
  void accept(
      List<UtilEscape.Range> ranges,
      Map<String, List<Integer>> namedSubstitutions,
      Map<Integer, String> singleSubstitutions);
}
