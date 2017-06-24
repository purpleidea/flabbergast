using System.Collections.Generic;
using System.Globalization;

namespace Flabbergast
{
    public class CharacterCategory : BaseMapFunctionInterop<string, Frame>
    {
        private static readonly Dictionary<UnicodeCategory, string> categories =
            new Dictionary<UnicodeCategory, string>
            {
                {UnicodeCategory.LowercaseLetter, "letter_lower"},
                {UnicodeCategory.ModifierLetter, "letter_modifier"},
                {UnicodeCategory.OtherLetter, "letter_other"},
                {UnicodeCategory.TitlecaseLetter, "letter_title"},
                {UnicodeCategory.UppercaseLetter, "letter_upper"},
                {UnicodeCategory.SpacingCombiningMark, "mark_combining"},
                {UnicodeCategory.EnclosingMark, "mark_enclosing"},
                {UnicodeCategory.NonSpacingMark, "mark_nonspace"},
                {UnicodeCategory.DecimalDigitNumber, "number_decimal"},
                {UnicodeCategory.LetterNumber, "number_letter"},
                {UnicodeCategory.OtherNumber, "number_other"},
                {UnicodeCategory.Control, "other_control"},
                {UnicodeCategory.Format, "other_format"},
                {UnicodeCategory.PrivateUse, "other_private"},
                {UnicodeCategory.Surrogate, "other_surrogate"},
                {UnicodeCategory.OtherNotAssigned, "other_unassigned"},
                {UnicodeCategory.ConnectorPunctuation, "punctuation_connector"},
                {UnicodeCategory.DashPunctuation, "punctuation_dash"},
                {UnicodeCategory.ClosePunctuation, "punctuation_end"},
                {UnicodeCategory.FinalQuotePunctuation, "punctuation_final_quote"},
                {UnicodeCategory.InitialQuotePunctuation, "punctuation_initial_quote"},
                {UnicodeCategory.OtherPunctuation, "punctuation_other"},
                {UnicodeCategory.OpenPunctuation, "punctuation_start"},
                {UnicodeCategory.LineSeparator, "separator_line"},
                {UnicodeCategory.ParagraphSeparator, "separator_paragraph"},
                {UnicodeCategory.SpaceSeparator, "separator_space"},
                {UnicodeCategory.CurrencySymbol, "symbol_currency"},
                {UnicodeCategory.MathSymbol, "symbol_math"},
                {UnicodeCategory.ModifierSymbol, "symbol_modifier"},
                {UnicodeCategory.OtherSymbol, "symbol_other"}
            };

        private readonly Dictionary<UnicodeCategory, object> mappings = new Dictionary<UnicodeCategory, object>();

        public CharacterCategory(TaskMaster task_master, SourceReference source_ref,
            Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container)
        {
        }

        protected override Frame ComputeResult(string input)
        {
            var frame = new MutableFrame(task_master, source_reference, context, container);
            for (var it = 0; it < input.Length; it++)
                frame.Set(it + 1, mappings[char.GetUnicodeCategory(input[it])]);
            return frame;
        }

        protected override void SetupExtra()
        {
            foreach (var entry in categories)
            {
                var key = entry.Key;
                var lookup = Find<object>(x => mappings[key] = x);
                lookup.AllowDefault();
                lookup.Lookup(entry.Value);
            }
        }
    }

    public class StringToCodepoints : BaseMapFunctionInterop<string, Frame>
    {
        public StringToCodepoints(TaskMaster task_master, SourceReference source_ref,
            Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container)
        {
        }

        protected override Frame ComputeResult(string input)
        {
            var frame = new MutableFrame(task_master, source_reference, context, container);
            for (var it = 0; it < input.Length; it++)
                frame.Set(it + 1, (long)char.ConvertToUtf32(input, it));
            return frame;
        }
    }
}