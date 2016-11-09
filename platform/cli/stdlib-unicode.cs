using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using System.Threading;

namespace Flabbergast {
public class CharacterCategory : Computation {
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

    private int interlock;
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
    protected override bool Run() {
        if (mappings.Count == 0) {
            interlock = categories.Count + 2;
            Computation input_lookup = new Lookup(task_master, source_reference, new [] {"arg"}, context);
            input_lookup.Notify(input_result => {
                if (input_result is Stringish) {
                    input = input_result.ToString();
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference, "Input argument must be a string.");
                }
            });

            foreach (var entry in categories) {
                var lookup = new Lookup(task_master, source_reference, new [] { entry.Value }, context);
                lookup.Notify(cat_result => {
                    mappings[entry.Key] = cat_result;
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                });
            }

            if (Interlocked.Decrement(ref interlock) > 0) {
                return false;
            }
        }
        var frame = new MutableFrame(task_master, source_reference, context, container);
        for (int it = 0; it < input.Length; it++) {
            frame.Set(it + 1, mappings[Char.GetUnicodeCategory(input[it])]);
        }
        result = frame;
        return true;
    }
}
public class StringToCodepoints : Computation {
    private int interlock = 2;
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
    protected override bool Run() {
        if (input == null) {
            Computation input_lookup = new Lookup(task_master, source_reference, new [] {"arg"}, context);
            input_lookup.Notify(input_result => {
                if (input_result is Stringish) {
                    input = input_result.ToString();
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference, "Input argument must be a string.");
                }
            });

            if (Interlocked.Decrement(ref interlock) > 0) {
                return false;
            }
        }
        var frame = new MutableFrame(task_master, source_reference, context, container);
        for (int it = 0; it < input.Length; it++) {
            frame.Set(it + 1, (long) Char.ConvertToUtf32(input, it));
        }
        result = frame;
        return true;
    }
}
public class Punycode : Computation {
    private int interlock;

    private String input;
    private System.Globalization.IdnMapping mapping = new System.Globalization.IdnMapping();
    private bool encode;

    private SourceReference source_reference;
    private Context context;
    private Frame container;

    public Punycode(TaskMaster task_master, SourceReference source_ref,
                    Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
        this.container = self;
    }
    protected override bool Run() {
        if (input == null) {
            interlock = 5;
            new Lookup(task_master, source_reference, new [] {"arg"}, context).Notify(input_result => {
                if (input_result is Stringish) {
                    input = input_result.ToString();
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference, "Input argument must be a string.");
                }
            });
            new Lookup(task_master, source_reference, new [] {"encode"}, context).Notify(input_result => {
                if (input_result is bool) {
                    encode = (bool) input_result;
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference, "“encode” argument must be a Boolean.");
                }
            });
            new Lookup(task_master, source_reference, new [] {"allow_unassigned"}, context).Notify(input_result => {
                if (input_result is bool) {
                    mapping.AllowUnassigned = (bool) input_result;
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference, "“allow_unassigned” argument must be a Boolean.");
                }
            });
            new Lookup(task_master, source_reference, new [] {"strict_ascii"}, context).Notify(input_result => {
                if (input_result is bool) {
                    mapping.UseStd3AsciiRules	= (bool) input_result;
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference, "“strict_ascii” argument must be a Boolean.");
                }
            });
            if (Interlocked.Decrement(ref interlock) > 0) {
                return false;
            }
        }
        try {
            result = new SimpleStringish(encode ? mapping.GetAscii(input) : mapping.GetUnicode(input));
            return true;
        } catch (ArgumentException e) {
            task_master.ReportOtherError(source_reference, "Invalid punycode: " + e.Message);
            return false;
        }
    }
}
}
