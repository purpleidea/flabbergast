package flabbergast.time;

import flabbergast.*;

import org.joda.time.DateTime;

public class LocalNow extends BaseTime {
    public LocalNow(TaskMaster task_master, SourceReference source_ref,
                    Context context, Frame self, Frame container) {
        super(task_master, source_ref, context, self, container);
    }
    @Override
    protected void run() {
        result = makeTime(new DateTime());
    }
}
