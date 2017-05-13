package flabbergast.interop;

import flabbergast.export.BaseFrameTransformer;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/** Alter a time and date */
final class TimeModify extends BaseFrameTransformer<ZonedDateTime> {
  static final Definition DEFINITION =
      create(
          AnyConverter.asDateTime(false),
          TimeModify::new,
          LookupAssistant.find(
              AnyConverter.asInt(false), (i, x) -> i.milliseconds = x, "milliseconds"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.seconds = x, "seconds"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.minutes = x, "minutes"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.hours = x, "hours"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.months = x, "months"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.days = x, "days"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.years = x, "years"));

  private long days;
  private long hours;
  private long milliseconds;
  private long minutes;
  private long months;
  private long seconds;
  private long years;

  private TimeModify() {}

  @Override
  protected void apply(
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      Name name,
      ZonedDateTime input) {
    future.complete(
        Any.of(
            Frame.of(
                future,
                sourceReference,
                context,
                input
                    .plus((int) milliseconds, ChronoUnit.MILLIS)
                    .plusSeconds((int) seconds)
                    .plusMinutes((int) minutes)
                    .plusHours((int) hours)
                    .plusDays((int) days)
                    .plusMonths((int) months)
                    .plusYears((int) years))));
  }
}
