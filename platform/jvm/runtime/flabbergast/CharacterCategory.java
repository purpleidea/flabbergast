package flabbergast;

import java.util.HashMap;
import java.util.Map;

public class CharacterCategory extends BaseMapFunctionInterop<String, Frame> {
  private static final Map<Byte, String> categories;

  static {
    categories = new HashMap<Byte, String>();
    categories.put(Character.LOWERCASE_LETTER, "letter_lower");
    categories.put(Character.MODIFIER_LETTER, "letter_modifier");
    categories.put(Character.OTHER_LETTER, "letter_other");
    categories.put(Character.TITLECASE_LETTER, "letter_title");
    categories.put(Character.UPPERCASE_LETTER, "letter_upper");
    categories.put(Character.COMBINING_SPACING_MARK, "mark_combining");
    categories.put(Character.ENCLOSING_MARK, "mark_enclosing");
    categories.put(Character.NON_SPACING_MARK, "mark_nonspace");
    categories.put(Character.DECIMAL_DIGIT_NUMBER, "number_decimal");
    categories.put(Character.LETTER_NUMBER, "number_letter");
    categories.put(Character.OTHER_NUMBER, "number_other");
    categories.put(Character.CONTROL, "other_control");
    categories.put(Character.FORMAT, "other_format");
    categories.put(Character.PRIVATE_USE, "other_private");
    categories.put(Character.SURROGATE, "other_surrogate");
    categories.put(Character.UNASSIGNED, "other_unassigned");
    categories.put(Character.CONNECTOR_PUNCTUATION, "punctuation_connector");
    categories.put(Character.DASH_PUNCTUATION, "punctuation_dash");
    categories.put(Character.END_PUNCTUATION, "punctuation_end");
    categories.put(Character.FINAL_QUOTE_PUNCTUATION, "punctuation_final_quote");
    categories.put(Character.INITIAL_QUOTE_PUNCTUATION, "punctuation_initial_quote");
    categories.put(Character.OTHER_PUNCTUATION, "punctuation_other");
    categories.put(Character.START_PUNCTUATION, "punctuation_start");
    categories.put(Character.LINE_SEPARATOR, "separator_line");
    categories.put(Character.PARAGRAPH_SEPARATOR, "separator_paragraph");
    categories.put(Character.SPACE_SEPARATOR, "separator_space");
    categories.put(Character.CURRENCY_SYMBOL, "symbol_currency");
    categories.put(Character.MATH_SYMBOL, "symbol_math");
    categories.put(Character.MODIFIER_SYMBOL, "symbol_modifier");
    categories.put(Character.OTHER_SYMBOL, "symbol_other");
  }

  private Map<Byte, Object> mappings = new HashMap<Byte, Object>();

  public CharacterCategory(
      TaskMaster task_master,
      SourceReference source_reference,
      Context context,
      Frame self,
      Frame container) {
    super(Frame.class, String.class, task_master, source_reference, context, self, container);
  }

  @Override
  protected Frame computeResult(String input) throws Exception {
    MutableFrame frame =
        new MutableFrame(
            task_master, source_reference,
            context, container);
    for (int it = 0; it < input.length(); it++) {
      frame.set(it + 1, mappings.get((byte) Character.getType(input.charAt(it))));
    }
    return frame;
  }

  @Override
  protected void setupExtra() {
    for (Map.Entry<Byte, String> entry : categories.entrySet()) {
      final Byte key = entry.getKey();
      Sink<Object> mapping_lookup = find(Object.class, x -> mappings.put(key, x));
      mapping_lookup.allowDefault(false, null);
      mapping_lookup.lookup(entry.getValue());
    }
  }
}
