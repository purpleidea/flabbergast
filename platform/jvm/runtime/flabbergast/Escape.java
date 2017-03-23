package flabbergast;
import java.io.UnsupportedEncodingException;
import java.lang.Comparable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Escape extends Future {
    private interface ConsumeChar {
        void invoke(int c);
    }
    private interface ConsumeString {
        void invoke(String str);
    }

    private class Range implements Comparable<Range> {
        int start;
        int end;
        String replacement;
        public Range(int start, int end, String replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
        public int compareTo(Range r) {
            return start - r.start;
        }
    }

    private Map<Integer, String> single_substitutions = new TreeMap<Integer, String>();
    private List<Range> ranges = new ArrayList<Range>();
    private Map<String, String> input = new TreeMap<String, String>();
    private SourceReference source_ref;
    private Context context;
    private Frame self;
    private AtomicInteger interlock = new AtomicInteger(3);
    private boolean state = false;

    public Escape(TaskMaster task_master, SourceReference source_ref,
                  Context context, Frame self, Frame container) {
        super(task_master);
        this.source_ref = source_ref;
        this.context = context;
        this.self = self;
    }

    private class HandleArgs implements ConsumeResult {
        @Override
        public void consume(Object result) {
            if (result instanceof Frame) {
                Frame input = (Frame) result;
                interlock.addAndGet(input.count());
                int index = 0;
                for (String name : input) {
                    final String target_name = name;
                    input.getOrSubscribe(name, new ConsumeResult() {
                        @Override
                        public void consume(Object arg) {
                            if (arg instanceof Stringish) {
                                Escape.this.input.put(target_name,
                                                      arg.toString());
                                if (interlock.decrementAndGet() == 0) {
                                    task_master.slot(Escape.this);
                                }
                            } else {
                                task_master.reportOtherError(
                                    source_ref,
                                    String.format(
                                        "Expected “args” to contain strings. Got %s instead.",
                                        SupportFunctions.nameForClass(arg
                                                                      .getClass())));
                            }
                        }
                    });
                }
                if (interlock.decrementAndGet() == 0) {
                    task_master.slot(Escape.this);
                }
            } else {
                task_master.reportOtherError(source_ref, String.format(
                                                 "Expected “args” to be a frame. Got %s instead.",
                                                 SupportFunctions.nameForClass(result.getClass())));
            }
        }
    }

    void handleRange(final Frame spec) {
        lookupChar(spec, "start", new ConsumeChar() {
            @Override
            public void invoke(final int start) {
                lookupChar(spec, "end", new ConsumeChar() {
                    @Override
                    public void invoke(final int end) {
                        lookupString(spec, "format_str", new ConsumeString() {
                            @Override
                            public void invoke(final String replacement) {
                                ranges.add(new Range(start, end, replacement));
                                if (interlock.decrementAndGet() == 0) {
                                    task_master.slot(Escape.this);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    void handleSubstition(final Frame spec) {
        lookupChar(spec, "char", new ConsumeChar() {
            @Override
            public void invoke(final int c) {
                lookupString(spec, "replacement", new ConsumeString() {
                    @Override
                    public void invoke(final String replacement) {
                        single_substitutions.put(c, replacement);
                        if (interlock.decrementAndGet() == 0) {
                            task_master.slot(Escape.this);
                        }
                    }
                });
            }
        });
    }

    private class HandleTransformation implements ConsumeResult {
        @Override
        public void consume(Object result) {
            if (result instanceof Frame) {
                final Frame frame = (Frame) result;
                Lookup lookup = new Lookup(task_master, source_ref,
                                           new String[] {"type"}, Context.prepend(frame, null));
                lookup.listen(new ConsumeResult() {
                    @Override
                    public void consume(Object type_result) {
                        if (type_result instanceof Long) {
                            long type = (Long) type_result;
                            switch ((int) type) {
                            case 0 :
                                handleSubstition(frame);
                                return;
                            case 1 :
                                handleRange(frame);
                                return;
                            }
                        }
                        task_master.reportOtherError(source_ref,
                                                     "Illegal transformation specified.");
                    }
                });
            } else {
                task_master.reportOtherError(source_ref,
                                             "Non-frame in transformation list.");
            }
        }
    }
    private class HandleTransformations implements ConsumeResult {
        @Override
        public void consume(Object result) {
            if (result instanceof Frame) {
                Frame input = (Frame) result;
                interlock.addAndGet(input.count());
                for (String name : input) {
                    input.getOrSubscribe(name, new HandleTransformation());
                }
                if (interlock.decrementAndGet() == 0) {
                    task_master.slot(Escape.this);
                }
            } else {
                task_master
                .reportOtherError(
                    source_ref,
                    String.format(
                        "Expected “transformations” to be a frame. Got %s instead.",
                        SupportFunctions.nameForClass(result
                                                      .getClass())));
            }
        }
    }

    void lookupChar(Frame frame, final String name, final ConsumeChar consume) {
        lookupString(frame, name, new ConsumeString() {
            @Override
            public void invoke(String str) {
                if (str.offsetByCodePoints(0, 1) == str.length()) {
                    consume.invoke(str.codePointAt(0));
                } else {
                    task_master.reportOtherError(source_ref, String.format(
                                                     "String “%s” for “%s” must be a single codepoint.",
                                                     str, name));
                }
            }
        });
    }

    void lookupString(Frame frame, final String name,
                      final ConsumeString consume) {
        Lookup lookup = new Lookup(task_master, source_ref, new String[] {name},
                                   Context.prepend(frame, null));
        lookup.listen(new ConsumeResult() {
            @Override
            public void consume(Object result) {
                if (result instanceof Stringish) {
                    String str = result.toString();
                    consume.invoke(str);
                } else {
                    task_master.reportOtherError(source_ref, String.format(
                                                     "Expected “%s” to be a string. Got %s instead.",
                                                     name, SupportFunctions.nameForClass(result.getClass())));
                }
            }
        });
    }

    @Override
    protected void run() {
        if (!state) {
            Lookup input_lookup = new Lookup(task_master, source_ref,
                                             new String[] {"args"}, context);
            input_lookup.listen(new HandleArgs());
            Lookup transformation_lookup = new Lookup(task_master, source_ref,
                    new String[] {"transformations"}, context);
            transformation_lookup.listen(new HandleTransformations());
            state = true;
            if (interlock.decrementAndGet() > 0) {
                return;
            }
        }
        try {
            MutableFrame output_frame = new MutableFrame(task_master,
                    source_ref, context, self);
            for (Entry<String, String> entry : input.entrySet()) {
                StringBuilder buffer = new StringBuilder();
                String in_str = entry.getValue();
                for (int it = 0; it < in_str.length(); it = in_str
                        .offsetByCodePoints(it, 1)) {
                    int c = in_str.codePointAt(it);
                    boolean is_surrogate = Character
                                           .isSupplementaryCodePoint(c);
                    String replacement = single_substitutions.get(c);
                    if (replacement != null) {
                        buffer.append(replacement);
                    } else {
                        boolean matched = false;
                        for (Range range : ranges) {
                            if (c >= range.start && c <= range.end) {
                                byte[] utf8 = in_str.substring(it,
                                                               it + (is_surrogate ? 2 : 1)).getBytes(
                                                  "UTF-8");
                                // The bit masks are to avoid sign extension.
                                buffer.append(String.format(
                                                  range.replacement,
                                                  c,
                                                  0xFFFF & (int) in_str.charAt(it),
                                                  is_surrogate ? 0xFFFF & (int) in_str
                                                  .charAt(it + 1) : 0,
                                                  0xFF & (int) utf8[0], utf8.length > 1
                                                  ? 0xFF & (int) utf8[1]
                                                  : 0, utf8.length > 2
                                                  ? 0xFF & (int) utf8[2]
                                                  : 0, utf8.length > 3
                                                  ? 0xFF & (int) utf8[3]
                                                  : 0));
                                matched = true;
                                break;
                            }
                        }
                        if (!matched) {
                            buffer.appendCodePoint(c);
                        }
                    }
                }
                output_frame.set(entry.getKey(),
                                 new SimpleStringish(buffer.toString()));
            }
            result = output_frame;
        } catch (UnsupportedEncodingException e) {
            task_master.reportOtherError(source_ref, e.getMessage());
        }
    }
}
