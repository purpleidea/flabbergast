package flabbergast;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

public class CreateTime extends BaseFunctionInterop<ZonedDateTime> {
  private long day;
  private long hour;
  private long millisecond;
  private long minute;
  private long month;
  private long second;
  private long year;
  private ZoneId zone;

  public CreateTime(
      TaskMaster task_master,
      SourceReference source_ref,
      Context context,
      Frame self,
      Frame container) {
    super(task_master, source_ref, context, self, container);
  }

  private <T> void add(Class<T> clazz, String name, Consumer<T> writer) {
    Sink<T> lookup = find(clazz, writer);
    lookup.allowDefault(false, null);
    lookup.lookup(name);
  }

  @Override
  protected ZonedDateTime computeResult() {
    return ZonedDateTime.of(
        (int) year,
        (int) month,
        (int) day,
        (int) hour,
        (int) minute,
        (int) second,
        (int) millisecond * 1000,
        zone);
  }

  @Override
  protected void setup() {

    Sink<Long> month_lookup = find(Long.class, x -> month = x);
    month_lookup.allowDefault(false, null);
    ObjectSink<Ptr<Long>> month_obj = month_lookup.allowObject(Ptr::get, Ptr::new);
    month_obj.add(
        Long.class, "ordinal", false, Ptr::set, "Can only use a frame from the “months” frame.");
    month_lookup.lookup("month");

    add(Long.class, "millisecond", x -> millisecond = x);
    add(Long.class, "second", x -> second = x);
    add(Long.class, "minute", x -> minute = x);
    add(Long.class, "hour", x -> hour = x);
    add(Long.class, "day", x -> day = x);
    add(Long.class, "year", x -> year = x);
    add(Boolean.class, "is_utc", x -> zone = x ? ZoneId.of("Z") : ZoneId.systemDefault());
  }
}
