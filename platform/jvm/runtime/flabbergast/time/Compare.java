package flabbergast.time;

import flabbergast.*;

import java.lang.Math;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class Compare extends BaseTime {
    private ZonedDateTime left;
    private ZonedDateTime right;
    private boolean first = true;

    public Compare(TaskMaster task_master, SourceReference source_ref,
                   Context context, Frame self, Frame container) {
        super(task_master, source_ref, context, self, container);
    }
    @Override
    protected void run() {
        if (first) {
            first = false;
            interlock.set(3);
            getTime(x -> left = x, "arg");
            getTime(x -> right = x, "to");
            if (interlock.decrementAndGet() > 0) {
                return;
            }
        }
        result = ChronoUnit.SECONDS.between(right, left);
    }
}
