using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using System.Threading;

namespace Flabbergast {
public class Escape : Computation {
    private delegate void ConsumeChar(int c);
    private delegate void ConsumeString(string str);

    public static string Quote(string input) {
        return input.Replace("{", "{{").Replace("}", "}}");
    }

    private class Range {
        internal int start;
        internal int end;
        internal string replacement;
    }

    private Dictionary<int, string> single_substitutions = new Dictionary<int, string>();
    private SortedList<int, Range> ranges = new SortedList<int, Range>();
    private Dictionary<string, string> input = new Dictionary<string, string>();
    private SourceReference source_ref;
    private Context context;
    private Frame self;
    private int interlock = 3;
    private bool state = false;

    public Escape(TaskMaster task_master, SourceReference source_ref,
                  Context context, Frame self, Frame container) : base(task_master) {
        this.source_ref = source_ref;
        this.context = context;
        this.self = self;
    }

    private void HandleArgs(object result) {
        if (result is Frame) {
            var input = (Frame) result;
            Interlocked.Add(ref interlock, (int) input.Count);
            foreach (var name in input.GetAttributeNames()) {
                var target_name = name;
                input.GetOrSubscribe(name, arg => {
                    if (arg is Stringish) {
                        this.input[target_name] = arg.ToString();
                        if (Interlocked.Decrement(ref interlock) == 0) {
                            task_master.Slot(this);
                        }
                    } else {
                        task_master.ReportOtherError(source_ref, String.Format("Expected “args” to contain strings. Got {0} instead.", Stringish.NameForType(arg.GetType())));
                    }
                });
            }
            if (Interlocked.Decrement(ref interlock) == 0) {
                task_master.Slot(this);
            }
        } else {
            task_master.ReportOtherError(source_ref, String.Format("Expected “args” to be a frame. Got {0} instead.", Stringish.NameForType(result.GetType())));
        }
    }

    void HandleRange(Frame spec) {
        LookupChar(spec, "start", start => {
            LookupChar(spec, "end", end => {
                LookupString(spec, "format_str", replacement => {
                    if (ranges.ContainsKey(start)) {
                        task_master.ReportOtherError(source_ref, String.Format("Duplicate range starting at {0}", start));
                        return;
                    }
                    ranges.Add(start, new Range() {
                        start = start, end = end, replacement = replacement
                    });
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                });
            });
        });
    }

    void HandleSubstition(Frame spec) {
        LookupChar(spec, "char", c => {
            LookupString(spec, "replacement", replacement => {
                single_substitutions[c] = replacement;
                if (Interlocked.Decrement(ref interlock) == 0) {
                    task_master.Slot(this);
                }
            });
        });
    }

    private void HandleTransformation(object result) {
        if (result is Frame) {
            var frame = (Frame) result;
            var lookup = new Lookup(task_master, source_ref, new [] {"type"}, Context.Prepend(frame, null));
            lookup.Notify(type_result => {
                if (type_result is long) {
                    switch ((long) type_result) {
                    case 0:
                        HandleSubstition(frame);
                        return;
                    case 1:
                        HandleRange(frame);
                        return;
                    }
                }
                task_master.ReportOtherError(source_ref, "Illegal transformation specified.");
            });
        } else {
            task_master.ReportOtherError(source_ref, "Non-frame in transformation list.");
        }
    }
    private void HandleTransformations(object result) {
        if (result is Frame) {
            var input = (Frame) result;
            Interlocked.Add(ref interlock, (int) input.Count);
            foreach (var name in input.GetAttributeNames()) {
                input.GetOrSubscribe(name, HandleTransformation);
            }
            if (Interlocked.Decrement(ref interlock) == 0) {
                task_master.Slot(this);
            }
        } else {
            task_master.ReportOtherError(source_ref, String.Format("Expected “transformations” to be a frame. Got {0} instead.", Stringish.NameForType(result.GetType())));
        }
    }

    void LookupChar(Frame frame, string name, ConsumeChar consume) {
        LookupString(frame, name, str => {
            if (str.Length == 1 || str.Length == 2 && Char.IsSurrogatePair(str, 0)) {
                consume(Char.ConvertToUtf32(str, 0));
            } else {
                task_master.ReportOtherError(source_ref, String.Format("String “{0}” for “{1}” must be a single codepoint.", str, name));
            }
        });
    }

    void LookupString(Frame frame, string name, ConsumeString consume) {
        var lookup = new Lookup(task_master, source_ref, new [] {name}, Context.Prepend(frame, null));
        lookup.Notify(result => {
            if (result is Stringish) {
                var str = result.ToString();
                consume(str);
            } else {
                task_master.ReportOtherError(source_ref, String.Format("Expected “{0}” to be a string. Got {1} instead.", name, Stringish.NameForType(result.GetType())));
            }
        });
    }

    protected override void Run() {
        if (!state) {
            var input_lookup = new Lookup(task_master, source_ref, new [] {"args"}, context);
            input_lookup.Notify(HandleArgs);
            var transformation_lookup = new Lookup(task_master, source_ref, new [] {"transformations"}, context);
            transformation_lookup.Notify(HandleTransformations);
            state = true;
            if (Interlocked.Decrement(ref interlock) > 0) {
                return ;
            }
        }
        var output_frame = new MutableFrame(task_master, source_ref, context, self);
        foreach (var entry in input) {
            var in_str = entry.Value;
            var buffer = new StringBuilder();
            for (var it = 0; it < in_str.Length; it += Char.IsSurrogatePair(in_str, it) ? 2 : 1) {
                var c = Char.ConvertToUtf32(in_str, it);
                var is_surrogate = Char.IsSurrogatePair(in_str, it);
                string replacement;
                if (single_substitutions.TryGetValue(c, out replacement)) {
                    buffer.Append(replacement);
                } else {
                    bool matched = false;
                    foreach (var range in ranges.Values) {
                        if (c >= range.start && c <= range.end) {
                            var utf8 = new byte[4];

                            Encoding.UTF8.GetBytes(in_str, it, is_surrogate ? 2 : 1, utf8, 0);
                            buffer.Append(String.Format(range.replacement, c, (int) in_str[it], is_surrogate ? (int) in_str[it + 1] : 0, (int) utf8[0], (int) utf8[1], (int) utf8[2], (int) utf8[3]));
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        buffer.Append(Char.ConvertFromUtf32(c));
                    }
                }
            }
            output_frame.Set(entry.Key, new SimpleStringish(buffer.ToString()));
        }
        result = output_frame;
    }
}
}
