using System;
using System.Collections.Generic;
using System.Globalization;
using System.Threading;

namespace Flabbergast.Time {
	public abstract class BaseTime : Computation {
		public static readonly Frame[] Days = MakeFrames(new []{"sunday", "monday", "tuesday", "wednesday", "thrusday", "friday", "saturday"}, CultureInfo.CurrentCulture.DateTimeFormat.AbbreviatedDayNames, CultureInfo.CurrentCulture.DateTimeFormat.DayNames);
		public static readonly Frame[] Months = MakeFrames(new []{"january", "februrary", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"}, CultureInfo.CurrentCulture.DateTimeFormat.AbbreviatedMonthNames, CultureInfo.CurrentCulture.DateTimeFormat.MonthNames);

		private static readonly SourceReference time_src = new NativeSourceReference("<the big bang>");
		private static readonly Frame AllDays = new FixedFrame("days", time_src) { Days };
		private static readonly Frame AllMonths = new FixedFrame("months", time_src) { Months };

		public static Frame GetDays() {
			return AllDays;
		}
		public static Frame GetMonths() {
			return AllMonths;
		}

		private static Frame[] MakeFrames(string[] attrs, string[] short_names, string[] long_names) {
			var result = new Frame[attrs.Length];
			for (var i = 0; i < attrs.Length; i++) {
				result[i] = new FixedFrame(attrs[i], time_src) {
					{ "short_name", short_names[i] },
					{ "long_name", long_names[i] },
					{ "ordinal", i + 1 }
				};
			}
			return result;
		}

		private static readonly Dictionary<string, Func<DateTime, object>> time_accessors = new Dictionary<string, Func<DateTime, object>> {
			{ "day_of_week", d => Days[(int) d.DayOfWeek] },
			{ "from_midnight", d => d.TimeOfDay.TotalSeconds },
			{ "milliseconds", d => (long) d.Millisecond },
			{ "second", d => (long) d.Second },
			{ "minute", d => (long) d.Minute },
			{ "hour", d => (long) d.Hour },
			{ "day", d => (long) d.Day },
			{ "month", d => Months[d.Month - 1] },
			{ "year", d => (long) d.Year },
			{ "week", d => (long) new GregorianCalendar().GetWeekOfYear(d, CalendarWeekRule.FirstDay, DayOfWeek.Sunday) },
			{ "day_of_year", d => (long) d.DayOfYear },
			{ "epoch",  d => (d - new DateTime(1970, 1, 1, 0, 0, 0, d.Kind)).TotalSeconds },
			{ "is_utc", d => d.Kind == DateTimeKind.Utc },
			{ "is_leap_year", d => DateTime.IsLeapYear(d.Year) }
		};

		public static Frame MakeTime(DateTime time, TaskMaster task_master) {
			return ReflectedFrame.Create<DateTime>(task_master, time, time_accessors);
		}

		protected int interlock;
		protected readonly SourceReference source_reference;
		protected readonly Context context;
		protected readonly Frame container;

		public BaseTime(TaskMaster task_master, SourceReference source_ref,
				Context context, Frame self, Frame container) : base(task_master) {
			this.source_reference = source_ref;
			this.context = context;
			this.container = self;
		}

	protected void GetUnixTime(Action<DateTime> target, Context context) {
			new Lookup(task_master, source_reference, new[]{"epoch"}, context).Notify(epoch => {
				if (epoch is Int64) {
					var time = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
					target(time.AddSeconds((long) epoch));
					if (Interlocked.Decrement(ref interlock) == 0) {
						task_master.Slot(this);
					}
				} else {
					task_master.ReportOtherError(source_reference, "“epoch” must be an Int.");
				}
			});
		}
		protected void GetTime(Action<DateTime> target, params string[] names) {
			Computation lookup = new Lookup(task_master, source_reference, names, context);
			lookup.Notify(result => {
				if (result is ReflectedFrame && ((ReflectedFrame) result).Backing is DateTime) {
					target((DateTime) ((ReflectedFrame) result).Backing);
					if (Interlocked.Decrement(ref interlock) == 0) {
						task_master.Slot(this);
					}
				} else if (result is Frame) {
					GetUnixTime(target, Context.Prepend((Frame) result, null));
				} else {
					task_master.ReportOtherError(source_reference, string.Format("“{0}” must be a Frame.", string.Join(".", names)));
				}
			});
		}

		protected Frame MakeTime(DateTime time) {
			return MakeTime(time, task_master);
		}
	}
	abstract class BaseParts : BaseTime {
		protected long milliseconds;
		protected long seconds;
		protected long minutes;
		protected long hours;
		protected long months;
		protected long days;
		protected long years;

		public BaseParts(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}

		protected void LookupParts(bool plural) {
 			var parts = new Dictionary<string, Action<long>> {
				{ "millisecond", x => milliseconds = x },
				{ "second", x => seconds = x },
				{ "minute", x => minutes = x },
				{ "hour", x => hours = x },
				{ "month", x => months = x },
				{ "day", x => days = x },
				{ "year", x => years = x }
			};
			Interlocked.Add(ref interlock, parts.Count);
			foreach(var entry in parts) {
				var name = entry.Key + (plural ? "s" : "");
				new Lookup(task_master, source_reference, new[]{ name }, context).Notify(result => {
					if (result is Int64) {
						entry.Value((long) result);
						if (Interlocked.Decrement(ref interlock) == 0) {
							task_master.Slot(this);
						}
					} else if (name == "month" && result is Frame) {
						new Lookup(task_master, source_reference, new[]{ "ordinal" }, Context.Prepend((Frame) result, null)).Notify(ord_result => {
							if (ord_result is Int64) {
								entry.Value((long) ord_result);
								if (Interlocked.Decrement(ref interlock) == 0) {
									task_master.Slot(this);
								}
							} else {
								task_master.ReportOtherError(source_reference, string.Format("“{0}.ordinal” must be an Int.", name));
							}
						});
					} else {
						task_master.ReportOtherError(source_reference, string.Format("“{0}” must be an Int.", name));
					}
				});
			}
		}
	}

	class Compare : BaseTime {
		private DateTime left;
		private DateTime right;
		private bool first = true;

		public Compare(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}
		protected override bool Run() {
			if (first) {
				first = false;
				interlock = 3;
				GetTime(x => left = x, "arg");
				GetTime(x => right = x, "to");
				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}
			result = (left - right).TotalSeconds;
			return true;
		}
	}
	class FromUnix : BaseTime {
		public FromUnix(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}
		protected override bool Run() {
			GetUnixTime(d => {
				result = MakeTime(d);
				WakeupListeners();
			}, context);
			return false;
		}
	}
	class FromParts : BaseParts {
		private bool first = true;
		private DateTimeKind kind;
		public FromParts(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}
		protected override bool Run() {
			if (first) {
				first = false;
				interlock = 2;
				new Lookup(task_master, source_reference, new[]{"is_utc"}, context).Notify(is_utc => {
					if (is_utc is bool) {
						kind = ((bool) is_utc) ? DateTimeKind.Utc : DateTimeKind.Local;
						if (Interlocked.Decrement(ref interlock) == 0) {
							task_master.Slot(this);
						}
					} else {
						task_master.ReportOtherError(source_reference, "“is_utc” must be an Bool.");
					}
				});
				LookupParts(false);
				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}
			result = MakeTime(new DateTime((int) years, (int) months, (int) days, (int) hours, (int) minutes, (int) seconds, (int) milliseconds, kind));
			return true;
		}
	}
	class LocalNow : BaseTime {
		public LocalNow(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}
		protected override bool Run() {
			result = MakeTime(DateTime.Now);
			return true;
		}
	}
	class Modify : BaseParts {
		private bool first = true;
		private DateTime initial;
		public Modify(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}
		protected override bool Run() {
			if (first) {
				first = false;
				interlock = 2;
				GetTime(d => initial = d, "arg");
				LookupParts(true);
				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}
			result = MakeTime(initial
				.AddMilliseconds(milliseconds)
				.AddSeconds(seconds)
				.AddMinutes(minutes)
				.AddHours(hours)
				.AddDays(days)
				.AddMonths((int) months)
				.AddYears((int) years));
			return true;
		}
	}
	class SwitchZone : BaseTime {
		private bool first = true;
		private DateTime initial;
		private bool to_utc;
		public SwitchZone(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}
		protected override bool Run() {
			if (first) {
				first = false;
				interlock = 3;
				GetTime(d => initial = d, "arg");
				new Lookup(task_master, source_reference, new[]{"to_utc"}, context).Notify(to_utc => {
					if (to_utc is bool) {
						this.to_utc = (bool) to_utc;
						if (Interlocked.Decrement(ref interlock) == 0) {
							task_master.Slot(this);
						}
					} else {
						task_master.ReportOtherError(source_reference, "“to_utc” must be an Bool.");
					}
				});
				if (Interlocked.Decrement(ref interlock) > 0) {
					return false;
				}
			}
			result = MakeTime(to_utc ? initial.ToUniversalTime() : initial.ToLocalTime());
			return true;
		}
	}
	class UtcNow : BaseTime {
		public UtcNow(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}
		protected override bool Run() {
			result = MakeTime(DateTime.UtcNow);
			return true;
		}
	}
}
