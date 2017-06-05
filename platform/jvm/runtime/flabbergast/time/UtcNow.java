package flabbergast.time;

import flabbergast.*;

import java.time.ZonedDateTime;
import java.time.ZoneId;

public class UtcNow extends BaseTime {
    public UtcNow(TaskMaster task_master, SourceReference source_ref,
                  Context context, Frame self, Frame container) {
        super(task_master, source_ref, context, self, container);
    }
    @Override
    protected void run() {
        result = makeTime(ZonedDateTime.now(ZoneId.of("Z")));
    }
}
