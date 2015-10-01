package flabbergast.time;

import flabbergast.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;

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
    interface ConsumeLong {
        void invoke(long l);
    }
    protected void lookupParts(boolean plural) {
        Map<String, ConsumeLong> parts = new HashMap<String, ConsumeLong>();
        parts.put("millisecond", new ConsumeLong() {
            @Override
            public void invoke(long x) {
                milliseconds = x;
            }
        });
        parts.put("second", new ConsumeLong() {
            @Override
            public void invoke(long x) {
                seconds = x;
            }
        });
        parts.put("minute", new ConsumeLong() {
            @Override
            public void invoke(long x) {
                minutes = x;
            }
        });
        parts.put("hour", new ConsumeLong() {
            @Override
            public void invoke(long x) {
                hours = x;
            }
        });
        parts.put("month", new ConsumeLong() {
            @Override
            public void invoke(long x) {
                months = x;
            }
        });
        parts.put("day", new ConsumeLong() {
            @Override
            public void invoke(long x) {
                days = x;
            }
        });
        parts.put("year", new ConsumeLong() {
            @Override
            public void invoke(long x) {
                years = x;
            }
        });
        interlock.addAndGet(parts.size());

        for (Entry<String, ConsumeLong> entry : parts.entrySet()) {
            final String name = entry.getKey() + (plural ? "s" : "");
            final ConsumeLong target = entry.getValue();
            new Lookup(task_master, source_reference, new String[] {name},
            context).listen(new ConsumeResult() {
                @Override
                public void consume(final Object result) {
                    if (result instanceof Long) {
                        target.invoke((Long) result);
                        if (interlock.decrementAndGet() == 0) {
                            task_master.slot(BaseParts.this);
                        }
                    } else if (name.equals("month") && result instanceof Frame) {
                        new Lookup(task_master, source_reference,
                                   new String[] {"ordinal"}, Context.prepend(
                                       (Frame) result, null))
                        .listen(new ConsumeResult() {
                            @Override
                            public void consume(Object ord_result) {
                                if (ord_result instanceof Long) {
                                    target.invoke((Long) ord_result);
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
                            }
                        });
                    } else {
                        task_master.reportOtherError(source_reference,
                                                     String.format("“%s” must be an Int.", name));
                    }
                }
            });
        }
    }
}
