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
				Computation input_lookup = new Lookup(task_master, source_reference, new []{"arg"}, context);
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
					var lookup = new Lookup(task_master, source_reference, new []{ entry.Value }, context);
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
			for(int it = 0; it < input.Length; it++) {
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
				Computation input_lookup = new Lookup(task_master, source_reference, new []{"arg"}, context);
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
			for(int it = 0; it < input.Length; it++) {
				frame.Set(it + 1, (long) Char.ConvertToUtf32(input, it));
			}
			result = frame;
			return true;
		}
	}

	public class ParseDouble : Computation {

		private int interlock = 2;
		private String input;

		private SourceReference source_reference;
		private Context context;

		public ParseDouble(TaskMaster master, SourceReference source_ref,
				Context context, Frame self, Frame container) : base(master) {
			this.source_reference = source_ref;
			this.context = context;
		}

		protected override bool Run() {
			if (input == null) {
				var input_lookup = new Lookup(task_master, source_reference,
						new String[]{"arg"}, context);
				input_lookup.Notify(input_result => {
					if (input_result is Stringish) {
						input = input_result.ToString();
						if (Interlocked.Decrement(ref interlock) == 0) {
							task_master.Slot(this);
						}
					} else {
						task_master.ReportOtherError(source_reference,
								"Input argument must be a string.");
					}
				});

				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}

			try {
				result = Convert.ToDouble(input);
				return true;
			} catch (Exception e) {
				task_master.ReportOtherError(source_reference, e.Message);
				return false;
			}
		}
	}

	public class ParseInt : Computation {

		private int interlock = 3;
		private String input;
		private int radix;

		private SourceReference source_reference;
		private Context context;

		public ParseInt(TaskMaster task_master, SourceReference source_ref,
				Context context, Frame self, Frame container) : base(task_master) {
			this.source_reference = source_ref;
			this.context = context;
		}

		protected override bool Run() {
			if (input == null) {
				var input_lookup = new Lookup(task_master, source_reference,
						new String[]{"arg"}, context);
				input_lookup.Notify(input_result => {
					if (input_result is Stringish) {
						input = input_result.ToString();
						if (Interlocked.Decrement(ref interlock) == 0) {
							task_master.Slot(this);
						}
					} else {
						task_master.ReportOtherError(source_reference,
								"Input argument must be a string.");
					}
				});

				var radix_lookup = new Lookup(task_master, source_reference,
						new String[]{"radix"}, context);
				radix_lookup.Notify(radix_result => {
					if (radix_result is Int64) {
						radix = (int)(long)radix_result;
						if (Interlocked.Decrement(ref interlock) == 0) {
							task_master.Slot(this);
						}
					} else {
						task_master.ReportOtherError(source_reference,
								"Input argument must be a string.");
					}
				});

				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}

			try {
				result = Convert.ToInt64(input, radix);
				return true;
			} catch (Exception e) {
				task_master.ReportOtherError(source_reference, e.Message);
				return false;
			}
		}
	}
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
		private string[] input;
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
					this.input = new string[input.Count];
					var index = 0;
					foreach (var name in input.GetAttributeNames()) {
						var target_index = index++;
						input.GetOrSubscribe(name, arg => {
							if (arg is Stringish) {
								this.input[target_index] = arg.ToString();
								if (Interlocked.Decrement(ref interlock) == 0) {
									task_master.Slot(this);
								}
							} else {
								task_master.ReportOtherError(source_ref, String.Format("Expected “args” to contain strings. Got {0} instead.", arg.GetType()));
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
						ranges.Add(start, new Range() { start = start, end = end, replacement = replacement });
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
				var lookup = new Lookup(task_master, source_ref, new []{"type"}, Context.Prepend(frame, null));
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
			var lookup = new Lookup(task_master, source_ref, new []{name}, Context.Prepend(frame, null));
			lookup.Notify(result => {
				if (result is Stringish) {
					var str = result.ToString();
					consume(str);
				} else {
					task_master.ReportOtherError(source_ref, String.Format("Expected “{0}” to be a string. Got {1} instead.", name, result.GetType()));
				}
			});
		}

		protected override bool Run() {
			if (!state) {
				var input_lookup = new Lookup(task_master, source_ref, new []{"args"}, context);
				input_lookup.Notify(HandleArgs);
				var transformation_lookup = new Lookup(task_master, source_ref, new []{"transformations"}, context);
				transformation_lookup.Notify(HandleTransformations);
				state = true;
				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}
			var output_frame = new MutableFrame(task_master, source_ref, context, self);
			for (var index = 0; index < input.Length; index++) {
				var buffer = new StringBuilder();
				for(var it = 0; it < input[index].Length; it += Char.IsSurrogatePair(input[index], it) ? 2 : 1) {
					var c = Char.ConvertToUtf32(input[index], it);
					var is_surrogate = Char.IsSurrogatePair(input[index], it);
					string replacement;
					if (single_substitutions.TryGetValue(c, out replacement)) {
						buffer.Append(replacement);
					} else {
						bool matched = false;
						foreach(var range in ranges.Values) {
							if (c >= range.start && c <= range.end) {
								var utf8 = new byte[4];
								
								Encoding.UTF8.GetBytes(input[index], it, is_surrogate ? 2 : 1, utf8, 0);
								buffer.Append(String.Format(range.replacement, c, (int) input[index][it], is_surrogate ? (int) input[index][it + 1] : 0, (int) utf8[0], (int) utf8[1], (int) utf8[2], (int) utf8[3]));
								matched = true;
								break;
							}
						}
						if (!matched) {
							buffer.Append(Char.ConvertFromUtf32(c));
						}
					}
				}
				output_frame.Set(index, new SimpleStringish(buffer.ToString()));
			}
			result = output_frame;
			return true;
		}
	}
}
