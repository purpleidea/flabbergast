package flabbergast.time;

import flabbergast.*;

import java.time.ZonedDateTime;
import java.time.ZoneId;

public class FromParts extends BaseParts {
    private boolean first = true;
    private ZoneId zone;
    public FromParts(TaskMaster task_master, SourceReference source_ref,
                     Context context, Frame self, Frame container) {
        super(task_master, source_ref, context, self, container);
    }
    @Override
    protected void run() {
        if (first) {
            first = false;
            interlock.set(2);
            new Lookup(task_master, source_reference, new String[] {"is_utc"},
            context).listen(is_utc -> {
                if (is_utc instanceof Boolean) {
                    zone = (Boolean) is_utc
                    ? ZoneId.of("Z")
                    : ZoneId.systemDefault();
                    if (interlock.decrementAndGet() == 0) {
                        task_master.slot(FromParts.this);
                    }
                } else {
                    task_master.reportOtherError(source_reference,
                    "“is_utc” must be an Bool.");
                }
            }
                                      );
            lookupParts(false);
            if (interlock.decrementAndGet() > 0) {
                return;
            }
        }
        result = makeTime(ZonedDateTime.of((int) years, (int) months, (int) days,
                                           (int) hours, (int) minutes, (int) seconds, (int) milliseconds * 1000,
                                           zone));
    }
}
