package flabbergast;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

public class CharacterCategory extends Computation implements ConsumeResult {
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
        categories
        .put(Character.CONNECTOR_PUNCTUATION, "punctuation_connector");
        categories.put(Character.DASH_PUNCTUATION, "punctuation_dash");
        categories.put(Character.END_PUNCTUATION, "punctuation_end");
        categories.put(Character.FINAL_QUOTE_PUNCTUATION,
                       "punctuation_final_quote");
        categories.put(Character.INITIAL_QUOTE_PUNCTUATION,
                       "punctuation_initial_quote");
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

    private class Category implements ConsumeResult {
        Byte code;

        public Category(Byte code) {
            this.code = code;
        }

        @Override
        public void consume(Object result) {
            mappings.put(code, result);
            if (interlock.decrementAndGet() == 0) {
                task_master.slot(CharacterCategory.this);
            }
        }
    }
    private Map<Byte, Object> mappings = new HashMap<Byte, Object>();

    private AtomicInteger interlock = new AtomicInteger();
    private String input;

    private SourceReference source_reference;
    private Context context;
    private Frame container;

    public CharacterCategory(TaskMaster task_master,
                             SourceReference source_ref, Context context, Frame self,
                             Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
        this.container = self;
    }
    @Override
    public void consume(Object result) {
        if (result instanceof Stringish) {
            input = result.toString();
            if (interlock.decrementAndGet() == 0) {
                task_master.slot(this);
            }
        } else {
            task_master.reportOtherError(source_reference,
                                         "Input argument must be a string.");
        }
    }

    @Override
    protected void run() {
        if (mappings.size() == 0) {
            interlock.set(categories.size() + 2);
            Computation input_lookup = new Lookup(task_master,
                                                  source_reference, new String[] {"arg"}, context);
            input_lookup.listen(this);
            for (Map.Entry<Byte, String> entry : categories.entrySet()) {
                Computation lookup = new Lookup(task_master, source_reference,
                                                new String[] {entry.getValue() }, context);
                lookup.listen(new Category(entry.getKey()));
            }
            if (interlock.decrementAndGet() > 0) {
                return;
            }
        }
        MutableFrame frame = new MutableFrame(task_master, source_reference,
                                              context, container);
        for (int it = 0; it < input.length(); it++) {
            frame.set(it + 1,
                      mappings.get((byte) Character.getType(input.charAt(it))));
        }
        result = frame;
    }
}
