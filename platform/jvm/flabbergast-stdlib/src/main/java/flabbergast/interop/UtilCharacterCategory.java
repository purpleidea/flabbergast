package flabbergast.interop;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import flabbergast.util.Pair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Get the Unicode character class of each codepoint in a string. */
class UtilCharacterCategory implements LookupAssistant.Recipient {
  public static final OverrideDefinition DEFINITION =
      LookupAssistant.create(
          AnyConverter.frameOf(AnyConverter.asString(false), false),
          UtilCharacterCategory::new,
          Stream.of(
                  Pair.of(Character.LOWERCASE_LETTER, "letter_lower"),
                  Pair.of(Character.MODIFIER_LETTER, "letter_modifier"),
                  Pair.of(Character.OTHER_LETTER, "letter_other"),
                  Pair.of(Character.TITLECASE_LETTER, "letter_title"),
                  Pair.of(Character.UPPERCASE_LETTER, "letter_upper"),
                  Pair.of(Character.COMBINING_SPACING_MARK, "mark_combining"),
                  Pair.of(Character.ENCLOSING_MARK, "mark_enclosing"),
                  Pair.of(Character.NON_SPACING_MARK, "mark_nonspace"),
                  Pair.of(Character.DECIMAL_DIGIT_NUMBER, "number_decimal"),
                  Pair.of(Character.LETTER_NUMBER, "number_letter"),
                  Pair.of(Character.OTHER_NUMBER, "number_other"),
                  Pair.of(Character.CONTROL, "other_control"),
                  Pair.of(Character.FORMAT, "other_format"),
                  Pair.of(Character.PRIVATE_USE, "other_private"),
                  Pair.of(Character.SURROGATE, "other_surrogate"),
                  Pair.of(Character.UNASSIGNED, "other_unassigned"),
                  Pair.of(Character.CONNECTOR_PUNCTUATION, "punctuation_connector"),
                  Pair.of(Character.DASH_PUNCTUATION, "punctuation_dash"),
                  Pair.of(Character.END_PUNCTUATION, "punctuation_end"),
                  Pair.of(Character.FINAL_QUOTE_PUNCTUATION, "punctuation_final_quote"),
                  Pair.of(Character.INITIAL_QUOTE_PUNCTUATION, "punctuation_initial_quote"),
                  Pair.of(Character.OTHER_PUNCTUATION, "punctuation_other"),
                  Pair.of(Character.START_PUNCTUATION, "punctuation_start"),
                  Pair.of(Character.LINE_SEPARATOR, "separator_line"),
                  Pair.of(Character.PARAGRAPH_SEPARATOR, "separator_paragraph"),
                  Pair.of(Character.SPACE_SEPARATOR, "separator_space"),
                  Pair.of(Character.CURRENCY_SYMBOL, "symbol_currency"),
                  Pair.of(Character.MATH_SYMBOL, "symbol_math"),
                  Pair.of(Character.MODIFIER_SYMBOL, "symbol_modifier"),
                  Pair.of(Character.OTHER_SYMBOL, "symbol_other"))
              .map(
                  entry -> {
                    final var key = entry.first().intValue();
                    final var name = entry.second();
                    return LookupAssistant.find((i, x) -> i.mappings.put(key, x), name);
                  }));

  private final Map<Name, String> args;
  private final Map<Integer, Any> mappings = new ConcurrentHashMap<>();

  private UtilCharacterCategory(Map<Name, String> args) {
    this.args = args;
  }

  private AttributeSource mapCharacter(String input) {
    return AttributeSource.listOfAny(
        input.codePoints().map(Character::getType).mapToObj(mappings::get));
  }

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    future.complete(
        Any.of(
            Frame.create(
                future,
                sourceReference,
                context,
                args.entrySet()
                    .stream()
                    .map(
                        entry ->
                            Attribute.of(
                                entry.getKey(),
                                Any.of(
                                    Frame.create(
                                        future,
                                        sourceReference,
                                        context,
                                        mapCharacter(entry.getValue())))))
                    .collect(AttributeSource.toSource()))));
  }
}
