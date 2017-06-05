package flabbergast.time;

import flabbergast.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.LongConsumer;

abstract class BaseParts extends BaseTime {
    protected long milliseconds;
    protected long seconds;
    protected long minutes;
    protected long hours;
    protected long months;
    protected long days;
    protected long years;

    public BaseParts(TaskMaster task_master, SourceReference source_ref,
                     Context context, Frame self, Frame container) {
        super(task_master, source_ref, context, self, container);
    }

    protected void lookupParts(boolean plural) {
        Map<String, LongConsumer> parts = new HashMap<>();
        parts.put("millisecond", x -> milliseconds = x);
        parts.put("second", x -> seconds = x);
        parts.put("minute", x -> minutes = x);
        parts.put("hour", x -> hours = x);
        parts.put("month", x -> months = x);
        parts.put("day", x -> days = x);
        parts.put("year", x -> years = x);
        interlock.addAndGet(parts.size());

        for (Entry<String, LongConsumer> entry : parts.entrySet()) {
            final String name = entry.getKey() + (plural ? "s" : "");
            final LongConsumer target = entry.getValue();
            new Lookup(task_master, source_reference, new String[] {name},
            context).listen(result -> {
                if (result instanceof Long) {
                    target.accept((Long) result);
                    if (interlock.decrementAndGet() == 0) {
                        task_master.slot(BaseParts.this);
                    }
                } else if (name.equals("month") && result instanceof Frame) {
                    new Lookup(task_master, source_reference,
                               new String[] {"ordinal"}, Context.prepend(
                                   (Frame) result, null))
                    .listen(ord_result -> {
                        if (ord_result instanceof Long) {
                            target.accept((Long) ord_result);
                            if (interlock.decrementAndGet() == 0) {
                                task_master
                                .slot(BaseParts.this);
                            }
                        } else {
                            task_master.reportOtherError(
                                source_reference,
                                String.format(
                                    "“%s.ordinal” must be an Int.",
                                    name));
                        }
                    });
                } else {
                    task_master.reportOtherError(source_reference,
                    String.format("“%s” must be an Int.", name));
                }
            });
        }
    }
}
