using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using System.Threading;

namespace Flabbergast {
public class CharacterCategory : Future {
    private static readonly Dictionary<UnicodeCategory, string> categories = new Dictionary<UnicodeCategory, string> {
        { UnicodeCategory.LowercaseLetter, "letter_lower" },
        { UnicodeCategory.ModifierLetter, "letter_modifier" },
        { UnicodeCategory.OtherLetter, "letter_other" },
        { UnicodeCategory.TitlecaseLetter, "letter_title" },
        { UnicodeCategory.UppercaseLetter, "letter_upper" },
        { UnicodeCategory.SpacingCombiningMark, "mark_combining" },
        { UnicodeCategory.EnclosingMark, "mark_enclosing" },
        { UnicodeCategory.NonSpacingMark, "mark_nonspace" },
        { UnicodeCategory.DecimalDigitNumber, "number_decimal" },
        { UnicodeCategory.LetterNumber, "number_letter" },
        { UnicodeCategory.OtherNumber, "number_other" },
        { UnicodeCategory.Control, "other_control" },
        { UnicodeCategory.Format, "other_format" },
        { UnicodeCategory.PrivateUse, "other_private" },
        { UnicodeCategory.Surrogate, "other_surrogate" },
        { UnicodeCategory.OtherNotAssigned, "other_unassigned" },
        { UnicodeCategory.ConnectorPunctuation, "punctuation_connector" },
        { UnicodeCategory.DashPunctuation, "punctuation_dash" },
        { UnicodeCategory.ClosePunctuation, "punctuation_end" },
        { UnicodeCategory.FinalQuotePunctuation, "punctuation_final_quote" },
        { UnicodeCategory.InitialQuotePunctuation, "punctuation_initial_quote" },
        { UnicodeCategory.OtherPunctuation, "punctuation_other" },
        { UnicodeCategory.OpenPunctuation, "punctuation_start" },
        { UnicodeCategory.LineSeparator, "separator_line" },
        { UnicodeCategory.ParagraphSeparator, "separator_paragraph" },
        { UnicodeCategory.SpaceSeparator, "separator_space" },
        { UnicodeCategory.CurrencySymbol, "symbol_currency" },
        { UnicodeCategory.MathSymbol, "symbol_math" },
        { UnicodeCategory.ModifierSymbol, "symbol_modifier" },
        { UnicodeCategory.OtherSymbol, "symbol_other" }
    };

    private Dictionary<UnicodeCategory, object> mappings = new Dictionary<UnicodeCategory, object>();

    private InterlockedLookup interlock;
    private String input;

    private SourceReference source_reference;
    private Context context;
    private Frame container;

    public CharacterCategory(TaskMaster task_master, SourceReference source_ref,
                             Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
        this.container = self;
    }
    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.LookupStr(x => this.input = x, "arg");
            foreach (var entry in categories) {
                var key = entry.Key;
                interlock.Lookup<object>(x => mappings[key] = x, entry.Value);
            }
        }
        if (!interlock.Away()) return;
        var frame = new MutableFrame(task_master, source_reference, context, container);
        for (int it = 0; it < input.Length; it++) {
            frame.Set(it + 1, mappings[Char.GetUnicodeCategory(input[it])]);
        }
        result = frame;
    }
}
public class StringToCodepoints : Future {
    private InterlockedLookup interlock;
    private String input;

    private SourceReference source_reference;
    private Context context;
    private Frame container;

    public StringToCodepoints(TaskMaster task_master, SourceReference source_ref,
                              Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
        this.container = self;
    }
    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.LookupStr(x => this.input = x, "arg");
        }
        if (!interlock.Away()) return;
        var frame = new MutableFrame(task_master, source_reference, context, container);
        for (int it = 0; it < input.Length; it++) {
            frame.Set(it + 1, (long) Char.ConvertToUtf32(input, it));
        }
        result = frame;
    }
}
public class Punycode : Future {
    private InterlockedLookup interlock;

    private String input;
    private System.Globalization.IdnMapping mapping = new System.Globalization.IdnMapping();
    private bool encode;

    private SourceReference source_reference;
    private Context context;

    public Punycode(TaskMaster task_master, SourceReference source_ref,
                    Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
    }
    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.LookupStr(x => input = x, "arg");
            interlock.Lookup<bool>(x => encode = x, "encode");
            interlock.Lookup<bool>(x => mapping.AllowUnassigned = x, "allow_unassigned");
            interlock.Lookup<bool>(x => mapping.UseStd3AsciiRules	= x, "strict_ascii");
        }
        if (!interlock.Away()) return;
        try {
            result = new SimpleStringish(encode ? mapping.GetAscii(input) : mapping.GetUnicode(input));
        } catch (ArgumentException e) {
            task_master.ReportOtherError(source_reference, "Invalid punycode: " + e.Message);
        }
    }
}
}
