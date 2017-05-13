package flabbergast.interop;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Create a time from “parts”. */
final class TimeCreate implements LookupAssistant.Recipient {
  static final Name ORDINAL = Name.of("ordinal");
  static final Definition DEFINITION =
      LookupAssistant.create(
          TimeCreate::new,
          LookupAssistant.find(
              AnyConverter.of(
                  AnyConverter.convertInt(value -> ConversionOperation.succeed((int) value)),
                  AnyConverter.convertFrameAttribute(
                      ORDINAL, AnyConverter.asInt(false).thenApply(Long::intValue))),
              (i, x) -> i.month = x,
              "month"),
          LookupAssistant.find(
              AnyConverter.asInt(false), (i, x) -> i.millisecond = x, "millisecond"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.second = x, "second"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.minute = x, "minute"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.hour = x, "hour"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.day = x, "day"),
          LookupAssistant.find(AnyConverter.asInt(false), (i, x) -> i.year = x, "year"),
          LookupAssistant.find(
              AnyConverter.asBool(false),
              (i, x) -> i.zone = x ? ZoneId.of("Z") : ZoneId.systemDefault(),
              "is_utc"));

  private long day;
  private long hour;
  private long millisecond;
  private long minute;
  private int month;
  private long second;
  private long year;
  private ZoneId zone;

  private TimeCreate() {}

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    future.complete(
        Any.of(
            Frame.of(
                future,
                sourceReference,
                context,
                ZonedDateTime.of(
                    (int) year,
                    month,
                    (int) day,
                    (int) hour,
                    (int) minute,
                    (int) second,
                    (int) millisecond * 1_000_000,
                    zone))));
  }
}
