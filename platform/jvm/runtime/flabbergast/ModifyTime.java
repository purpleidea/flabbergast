package flabbergast;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public class ModifyTime extends BaseMapFunctionInterop<ZonedDateTime, ZonedDateTime> {
  private long days;
  private long hours;
  private long milliseconds;
  private long minutes;
  private long months;
  private long seconds;
  private long years;

  public ModifyTime(
      TaskMaster task_master,
      SourceReference source_ref,
      Context context,
      Frame self,
      Frame container) {
    super(
        ZonedDateTime.class,
        ZonedDateTime.class,
        task_master,
        source_ref,
        context,
        self,
        container);
  }

  @Override
  protected ZonedDateTime computeResult(ZonedDateTime initial) {
    return initial
        .plus((int) milliseconds, ChronoUnit.MILLIS)
        .plusSeconds((int) seconds)
        .plusMinutes((int) minutes)
        .plusHours((int) hours)
        .plusDays((int) days)
        .plusMonths((int) months)
        .plusYears((int) years);
  }

  private void lookupDelta(String name, Consumer<Long> writer) {
    Sink<Long> delta_lookup = find(Long.class, writer);
    delta_lookup.allowDefault(false, null);
    delta_lookup.lookup(name);
  }

  @Override
  protected void setupExtra() {
    lookupDelta("milliseconds", x -> milliseconds = x);
    lookupDelta("seconds", x -> seconds = x);
    lookupDelta("minutes", x -> minutes = x);
    lookupDelta("hours", x -> hours = x);
    lookupDelta("months", x -> months = x);
    lookupDelta("days", x -> days = x);
    lookupDelta("years", x -> years = x);
  }
}
