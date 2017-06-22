package flabbergast;
import java.lang.Comparable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntUnaryOperator;

public class Escape extends BaseMapFunctionInterop<String, String> {

    public interface RangeAction extends BiConsumer<StringBuilder, Integer> {}

    interface DefaultConsumer extends BiConsumer<StringBuilder, Integer> {
        public boolean matches(int codepoint);
    }
    static class Range implements DefaultConsumer, Comparable<Range> {
        private final int start;
        private final int end;
        private final List<RangeAction> replacement;
        public Range(int start, int end, List<RangeAction> replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
        public void accept(StringBuilder buffer, Integer codepoint) {
            for (RangeAction action : replacement) {
                action.accept(buffer, codepoint);
            }
        }
        public int compareTo(Range r) {
            return start - r.start;
        }

        public boolean matches(int codepoint) {
            return start <= codepoint && codepoint <= end;
        }
    }


    enum CharFormat {
        HEX_LOWER {
            @Override
            public String get(int bits) {
                switch (bits) {
                case 32:
                            return "%08x";
                    case 16:
                        return "%04x";
                    case 8:
                        return "%02x";
                    default:
                        throw new IllegalArgumentException();
                    }
                }

            },
            HEX_UPPER {
            @Override
            public String get(int bits) {
                switch (bits) {
                case 32:
                    return "%08X";
                case 16:
                    return "%04X";
                case 8:
                    return "%02X";
                default:
                    throw new IllegalArgumentException();
                }
            }
        },  DECIMAL {
            @Override
            public String get(int bits) {
                return "%d";
            }
        };
        public abstract String get(int bits);
    }

    public static ComputeValue create(Map<Integer, String> single_substitutions, List<Range> ranges) {
        return (task_master, source_reference, context, self, container) -> new Escape(single_substitutions, ranges, task_master, source_reference, context, self, container);
    }
    public static void createUnicodeActions(BiConsumer<String, Frame> consumer) {
        for (CharFormat format : CharFormat.values()) {
            add(consumer, 32, 0, format, IntUnaryOperator.identity());
// http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-appendixa
            add(consumer, 16, 0, format, c -> ((c < 65536) ? c : ((c - 65536) % 1024 + 56320) % 65536));
            add(consumer, 16, 1, format, c -> (c < 65536) ? 0 : ((c - 65536) / 1024 + 55296) % 65536);
            add(consumer, 8, 0, format,  c -> ((c <= 127) ? c : ((c <= 2047) ? (c / 64 + 192) : ((c <= 65535) ? (c / 4096 + 224) : (c / 262144 + 240)))) % 256);
            add(consumer, 8, 1, format, c -> ((c <= 127) ? 0 : ((c <= 2047) ? (c % 64 + 128) : ((c <= 65535) ? ((c / 64) % 64 + 128) : ((c % 262144) * 4096 + 128)))) % 256);
            add(consumer, 8, 2, format, c -> ((c <= 2047) ? 0 : ((c <= 65535) ? (c % 64 + 128) : ((c % 4096) / 64 + 128))) % 256);
            add(consumer, 8, 3,  format, c -> ((c <= 65535) ? 0 : (c % 64 + 128)) % 256);
        }
    }
    private static void add(BiConsumer<String, Frame> consumer, int bits, int index, CharFormat format, IntUnaryOperator encode) {
        String path = "utils/str/escape/utf" + bits + "/" + index + "/" + format.name().toLowerCase();
        String name = "utf" + bits + "_"  + index + "_" + format.name().toLowerCase();
        RangeAction action = (buffer, codepoint) -> buffer.append(String.format(format.get(bits), encode.applyAsInt(codepoint)));

        consumer.accept(path, ReflectedFrame.create(name, action, Collections.emptyMap()));
    }

    private static final DefaultConsumer DEFAULT =
    new DefaultConsumer() {
        public void accept(StringBuilder builder, Integer codepoint) {
            builder.appendCodePoint(codepoint);
        }
        public boolean matches(int codepoint) {
            return true;
        }
    };

    private final Map<Integer, String> single_substitutions;
    private final List<DefaultConsumer> ranges = new ArrayList<>();
    public Escape(Map<Integer, String> single_substitutions, List<Range> ranges, TaskMaster task_master, SourceReference source_reference,
                  Context context, Frame self, Frame container) {
        super(String.class, String.class, task_master, source_reference, context, self, container);
        this.single_substitutions = single_substitutions;
        this.ranges.addAll(ranges);
    }

    @Override
    protected void setupExtra() {}

    @Override
    protected String computeResult(String input) {
        StringBuilder buffer = new StringBuilder();
        for (int it = 0; it < input.length(); it = input
                .offsetByCodePoints(it, 1)) {
            int c = input.codePointAt(it);
            if (single_substitutions.containsKey(c)) {
                buffer.append(single_substitutions.get(c));
            } else {
                ranges.stream().filter(r -> r.matches(c)).findFirst().orElse(DEFAULT).accept(buffer, c);
            }
        }
        return buffer.toString();
    }
}
