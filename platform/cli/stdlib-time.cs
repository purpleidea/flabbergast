using System;
using System.Globalization;

namespace Flabbergast {
class CreateTime : BaseFunctionInterop<DateTime> {
    private long millisecond;
    private long second;
    private long minute;
    private long hour;
    private long month;
    private long day;
    private long year;
    private DateTimeKind kind;

    public CreateTime(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}

    private  void Add<T>(string name, Action<T> writer) {
        var lookup = Find<T>(writer);
        lookup.AllowDefault(null);
        lookup.Lookup(name);
    }

    protected override void Setup() {
        var month_lookup = Find<long>(x => month = x);
        month_lookup.AllowDefault(null);
        var month_obj = month_lookup.AllowObject<Holder<long>>(x => x.Value);
        month_obj.Add<long>("ordinal", (x, v) => x.Value = v, "Can only use a frame from the “months” frame.");
        month_lookup.Lookup("month");

        Add<long>("millisecond", x => millisecond = x);
        Add<long>("second", x => second = x);
        Add<long>("minute", x => minute = x);
        Add<long>("hour", x => hour = x);
        Add<long>("day", x => day = x);
        Add<long>("year", x => year = x);
        Add<bool>("is_utc", x => kind = x ? DateTimeKind.Utc : DateTimeKind.Local);
    }

    protected override DateTime ComputeResult() {
        return new DateTime((int) year, (int) month, (int) day, (int) hour, (int) minute, (int) second, (int) millisecond, kind);
    }
}

class ModifyTime : BaseMapFunctionInterop<DateTime, DateTime> {
    private long milliseconds;
    private long seconds;
    private long minutes;
    private long hours;
    private long months;
    private long days;
    private long years;

    public ModifyTime(TaskMaster task_master, SourceReference source_ref, Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {}

    private void LookupDelta(string name, Action<long> writer) {
        var delta_lookup = Find<long>(writer);
        delta_lookup.AllowDefault(null);
        delta_lookup.Lookup(name);
    }

    protected override void SetupExtra() {
        LookupDelta("milliseconds", x => milliseconds = x);
        LookupDelta("seconds", x => seconds = x);
        LookupDelta("minutes", x => minutes = x);
        LookupDelta("hours", x => hours = x);
        LookupDelta("months", x => months = x);
        LookupDelta("days", x => days = x);
        LookupDelta("years", x => years = x);
    }

    protected override DateTime ComputeResult(DateTime initial) {
        return initial
               .AddMilliseconds(milliseconds)
               .AddSeconds(seconds)
               .AddMinutes(minutes)
               .AddHours(hours)
               .AddDays(days)
               .AddMonths((int) months)
               .AddYears((int) years);
    }
}
}
