using System;
using System.Collections.Generic;
using System.Globalization;
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

		private TaskMaster master;
		private SourceReference source_reference;
		private Context context;
		private Frame container;

		public CharacterCategory(TaskMaster master, SourceReference source_ref,
				Context context, Frame self, Frame container) {
			this.master = master;
			this.source_reference = source_ref;
			this.context = context;
			this.container = self;
		}
		protected override bool Run() {
			if (mappings.Count == 0) {
				interlock = categories.Count + 2;
				Computation input_lookup = new Lookup(master, source_reference, new []{"arg"}, context);
				input_lookup.Notify(input_result => {
					if (input_result is Stringish) {
						input = input_result.ToString();
						if (Interlocked.Decrement(ref interlock) == 0) {
							master.Slot(this);
						}
					} else {
						master.ReportOtherError(source_reference, "Input argument must be a string.");
					}
				});
				master.Slot(input_lookup);

				foreach (var entry in categories) {
					var lookup = new Lookup(master, source_reference, new []{ entry.Value }, context);
					lookup.Notify(cat_result => {
						mappings[entry.Key] = cat_result;
						if (Interlocked.Decrement(ref interlock) == 0) {
							master.Slot(this);
						}
					});
					master.Slot(lookup);
				}

				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}
			var frame = new Frame(master, master.NextId(), source_reference, context, container);
			for(int it = 0; it < input.Length; it++) {
				frame[TaskMaster.OrdinalNameStr(it + 1)] = mappings[Char.GetUnicodeCategory(input[it])];
			}
			result = frame;
			return true;
		}
	}
	public class StringToCodepoints : Computation {
		private int interlock = 2;
		private String input;

		private TaskMaster master;
		private SourceReference source_reference;
		private Context context;
		private Frame container;

		public StringToCodepoints(TaskMaster master, SourceReference source_ref,
				Context context, Frame self, Frame container) {
			this.master = master;
			this.source_reference = source_ref;
			this.context = context;
			this.container = self;
		}
		protected override bool Run() {
			if (input == null) {
				Computation input_lookup = new Lookup(master, source_reference, new []{"arg"}, context);
				input_lookup.Notify(input_result => {
					if (input_result is Stringish) {
						input = input_result.ToString();
						if (Interlocked.Decrement(ref interlock) == 0) {
							master.Slot(this);
						}
					} else {
						master.ReportOtherError(source_reference, "Input argument must be a string.");
					}
				});
				master.Slot(input_lookup);

				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}
			var frame = new Frame(master, master.NextId(), source_reference, context, container);
			for(int it = 0; it < input.Length; it++) {
				frame[TaskMaster.OrdinalNameStr(it + 1)] = (long) Char.ConvertToUtf32(input, it);
			}
			result = frame;
			return true;
		}
	}

	public class ParseDouble : Computation {

		private int interlock = 2;
		private String input;

		private TaskMaster master;
		private SourceReference source_reference;
		private Context context;

		public ParseDouble(TaskMaster master, SourceReference source_ref,
				Context context, Frame self, Frame container) {
			this.master = master;
			this.source_reference = source_ref;
			this.context = context;
		}

		protected override bool Run() {
			if (input == null) {
				var input_lookup = new Lookup(master, source_reference,
						new String[]{"arg"}, context);
				input_lookup.Notify(input_result => {
					if (input_result is Stringish) {
						input = input_result.ToString();
						if (Interlocked.Decrement(ref interlock) == 0) {
							master.Slot(this);
						}
					} else {
						master.ReportOtherError(source_reference,
								"Input argument must be a string.");
					}
				});
				master.Slot(input_lookup);

				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}

			try {
				result = Convert.ToDouble(input);
				return true;
			} catch (Exception e) {
				master.ReportOtherError(source_reference, e.Message);
				return false;
			}
		}
	}

	public class ParseInt : Computation {

		private int interlock = 3;
		private String input;
		private int radix;

		private TaskMaster master;
		private SourceReference source_reference;
		private Context context;

		public ParseInt(TaskMaster master, SourceReference source_ref,
				Context context, Frame self, Frame container) {
			this.master = master;
			this.source_reference = source_ref;
			this.context = context;
		}

		protected override bool Run() {
			if (input == null) {
				var input_lookup = new Lookup(master, source_reference,
						new String[]{"arg"}, context);
				input_lookup.Notify(input_result => {
					if (input_result is Stringish) {
						input = input_result.ToString();
						if (Interlocked.Decrement(ref interlock) == 0) {
							master.Slot(this);
						}
					} else {
						master.ReportOtherError(source_reference,
								"Input argument must be a string.");
					}
				});
				master.Slot(input_lookup);

				var radix_lookup = new Lookup(master, source_reference,
						new String[]{"radix"}, context);
				radix_lookup.Notify(radix_result => {
					if (radix_result is Int64) {
						radix = (int)(long)radix_result;
						if (Interlocked.Decrement(ref interlock) == 0) {
							master.Slot(this);
						}
					} else {
						master.ReportOtherError(source_reference,
								"Input argument must be a string.");
					}
				});
				master.Slot(radix_lookup);

				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}

			try {
				result = Convert.ToInt64(input, radix);
				return true;
			} catch (Exception e) {
				master.ReportOtherError(source_reference, e.Message);
				return false;
			}
		}
	}
}
