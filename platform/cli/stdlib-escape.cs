using System;
using System.Linq;
using System.Collections.Generic;
using System.Globalization;
using System.Text;

namespace Flabbergast {

public class EscapeBuilder : InterlockedLookup {
    public static int StringToCodepoint(string str) {
        if (str.Length == 0) {
            throw new ArgumentException("Empty string in transformation.");
        }
        if ((Char.IsSurrogatePair(str, 0) ? 2 : 1) < str.Length) {
            throw new ArgumentException(string.Format(
                                            "string “{0}” must be a single character.",
                                            str));
        }
        return Char.ConvertToUtf32(str, 0);
    }

    internal readonly Dictionary<int, string> single_substitutions = new Dictionary<int, string>();
    internal readonly List<Escape.Range> ranges = new List<Escape.Range>();
    private readonly Frame self;
    public EscapeBuilder(TaskMaster task_master, SourceReference source_reference,
                         Context context, Frame self, Frame container) : base(task_master, source_reference, context) {
        this.self = self;
    }


    protected override void Setup() {
        var transformations_lookup = FindAll<Action<EscapeBuilder>>(input => {
            foreach (var x in input.Values) {
                x(this);
            }
        });
        transformations_lookup.AllowDefault(null);
        transformations_lookup.Lookup("arg_values");
    }
    protected override void Resolve() {
        ranges.Sort();
        var tmpl = new Template(new BasicSourceReference("Make escape template", "<escape>", 0, 0, 0, 0, source_reference), context, self);
        tmpl["value"] = Escape.Create(single_substitutions, ranges);
        result = tmpl;
    }
}
public class EscapeCharacterBuilder : BaseReflectedInterop<Action<EscapeBuilder>> {
    private string character;
    private string replacement;
    public EscapeCharacterBuilder(TaskMaster task_master, SourceReference source_reference,
                                  Context context, Frame self, Frame container) :
    base(task_master, source_reference, context, self, container) {
    }

    protected override Dictionary<string, Func<Action<EscapeBuilder>, object>> GetAccessors() {
        return new Dictionary<string, Func<Action<EscapeBuilder>, object>>();
    }
    protected override Action<EscapeBuilder> ComputeResult() {
        var codepoint = EscapeBuilder.StringToCodepoint(character);
        return (builder) => builder.single_substitutions[codepoint] =  replacement;
    }

    protected override void Setup() {
        var char_lookup = Find<string>(x => character = x);
        char_lookup.AllowDefault(null);
        char_lookup.Lookup("char");
        var replacement_lookup = Find<string>(x => replacement = x);
        replacement_lookup.AllowDefault(null);
        replacement_lookup.Lookup("replacement");
    }
}
public class Escape : BaseMapFunctionInterop<string, string> {

    public delegate void RangeAction(StringBuilder buffer, int codepoint);

    public interface DefaultConsumer {
        void Accept(StringBuilder buffer, int codepoint);
        bool Matches(int codepoint);
    }
    public class Range : DefaultConsumer, IComparable<Range> {
        private readonly int start;
        private readonly int end;
        private readonly List<RangeAction> replacement;
        public Range(int start, int end, List<RangeAction> replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
        public void Accept(StringBuilder buffer, int codepoint) {
            foreach (RangeAction action in replacement) {
                action(buffer, codepoint);
            }
        }
        public int CompareTo(Range r) {
            return start - r.start;
        }

        public bool Matches(int codepoint) {
            return start <= codepoint && codepoint <= end;
        }
    }


    enum CharFormat { HEX_LOWER, HEX_UPPER, DECIMAL }
    private  static string GetFormat(CharFormat format, int bits) {
        switch (format) {
        case CharFormat.HEX_LOWER:
                    switch (bits) {
                case 32:
                            return "{0:x8}";
                    case 16:
                        return "{0:x4}";
                    case 8:
                        return "{0:x2}";
                    default:
                        throw new ArgumentException();
                    }
            case CharFormat.HEX_UPPER:
                switch (bits) {
            case 32:
                return "{0:X8}";
            case 16:
                return "{0:X4}";
            case 8:
                return "{0:X2}";
            default:
                throw new ArgumentException();
            }
        case CharFormat.DECIMAL:
            return "{0:D}";
        default:
            throw new ArgumentException();
        }
    }

    public static ComputeValue Create(Dictionary<int, string> single_substitutions, List<Range> ranges) {
        return (task_master, source_reference, context, self, container) => new Escape(single_substitutions, ranges, task_master, source_reference, context, self, container);
    }
    public static void CreateUnicodeActions(Action<string, Frame> consumer) {
        foreach (CharFormat format in Enum.GetValues(typeof(CharFormat))) {
            Add(consumer, 32, 0, format, c => c);
// http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-appendixa
            Add(consumer, 16, 0, format, c => ((c < 65536) ? c : ((c - 65536) % 1024 + 56320) % 65536));
            Add(consumer, 16, 1, format, c => (c < 65536) ? 0 : ((c - 65536) / 1024 + 55296) % 65536);
            Add(consumer, 8, 0, format,  c => ((c <= 127) ? c : ((c <= 2047) ? (c / 64 + 192) : ((c <= 65535) ? (c / 4096 + 224) : (c / 262144 + 240)))) % 256);
            Add(consumer, 8, 1, format, c => ((c <= 127) ? 0 : ((c <= 2047) ? (c % 64 + 128) : ((c <= 65535) ? ((c / 64) % 64 + 128) : ((c % 262144) * 4096 + 128)))) % 256);
            Add(consumer, 8, 2, format, c => ((c <= 2047) ? 0 : ((c <= 65535) ? (c % 64 + 128) : ((c % 4096) / 64 + 128))) % 256);
            Add(consumer, 8, 3,  format, c => ((c <= 65535) ? 0 : (c % 64 + 128)) % 256);
        }
    }
    private static void Add(Action<string, Frame> consumer, int bits, int index, CharFormat format, Func<int, int> encode) {
        string format_name = Enum.GetName(typeof(CharFormat), format).ToLower();
        string path = "utils/str/escape/utf" + bits + "/" + index + "/" + format_name;
        string name = "utf" + bits + "_"  + index  + "_" + format_name;
        RangeAction action = (buffer, codepoint) => buffer.Append(string.Format(GetFormat(format, bits), encode(codepoint)));

        consumer(path, ReflectedFrame.Create<RangeAction>(name, action, new Dictionary<string, Func<RangeAction, object>>()));
    }

    private class CopyConsumer : DefaultConsumer {
        public void Accept(StringBuilder builder, int codepoint) {
            builder.Append(Char.ConvertFromUtf32(codepoint));
        }
        public bool Matches(int codepoint) {
            return true;
        }
    };

    private readonly Dictionary<int, string> single_substitutions;
    private readonly List<DefaultConsumer> ranges = new List<DefaultConsumer>();
    public Escape(Dictionary<int, string> single_substitutions, List<Range> ranges, TaskMaster task_master, SourceReference source_reference,
                  Context context, Frame self, Frame container)         : base(task_master, source_reference, context, self, container) {
        this.single_substitutions = single_substitutions;
        this.ranges.AddRange(ranges);
    }

    protected override void SetupExtra() {}
    protected override string ComputeResult(string input) {
        DefaultConsumer copy_consumer = new CopyConsumer();
        StringBuilder buffer = new StringBuilder();
        for (int it = 0; it < input.Length; it += Char.IsSurrogatePair(input, it) ? 2 : 1) {
            int c = Char.ConvertToUtf32(input, it);
            if (single_substitutions.ContainsKey(c)) {
                buffer.Append(single_substitutions[c]);
            } else {
                (ranges.FirstOrDefault(r => r.Matches(c)) ?? copy_consumer).Accept(buffer, c);
            }
        }
        return buffer.ToString();
    }
}

public class EscapeRangeBuilder : BaseReflectedInterop<Action<EscapeBuilder>> {
    private string start;
    private string end;
    private List<Escape.RangeAction> actions;

    public EscapeRangeBuilder(TaskMaster task_master, SourceReference source_reference,
                              Context context, Frame self, Frame container) : base(task_master, source_reference, context, self, container) {

    }
    protected override Dictionary<string, Func<Action<EscapeBuilder>, object>> GetAccessors() {
        return new Dictionary<string, Func<Action<EscapeBuilder>, object>> ();
    }
    protected override Action<EscapeBuilder> ComputeResult() {
        var start_codepoint = EscapeBuilder.StringToCodepoint(start);
        var end_codepoint = EscapeBuilder.StringToCodepoint(end);
        if (start_codepoint > end_codepoint) {
            throw new ArgumentException("Transformation range has start before end.");
        }
        Escape.Range range = new Escape.Range(start_codepoint, end_codepoint, actions);
        return builder => builder.ranges.Add(range);
    }
    protected override void Setup() {
        var start_lookup = Find<string>(x => start = x);
        start_lookup.AllowDefault(null);
        start_lookup.Lookup("start");
        var end_lookup = Find<string>(x => end = x);
        end_lookup.AllowDefault(null);
        end_lookup.Lookup("end");
        var actions_lookup = FindAll<Escape.RangeAction>(x => actions = new List<Escape.RangeAction>(x.Values));
        actions_lookup.AllowDefault("Frame is not one of the Unicode escapes known.");
        actions_lookup.Allow<string>(str => (buffer, codepoint) => buffer.Append(str), null);
        actions_lookup.Lookup("replacement");
    }
}
}
