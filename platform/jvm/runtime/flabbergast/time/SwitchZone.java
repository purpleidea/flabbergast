package flabbergast.time;

import flabbergast.*;

import java.time.ZonedDateTime;
import java.time.ZoneId;

public class SwitchZone extends BaseTime {
    private boolean first = true;
    private ZonedDateTime initial;
    private boolean to_utc;
    public SwitchZone(TaskMaster task_master, SourceReference source_ref,
                      Context context, Frame self, Frame container) {
        super(task_master, source_ref, context, self, container);
    }
    @Override
    protected void run() {
        if (first) {
            first = false;
            interlock.set(3);
            getTime(x -> initial = x, "arg");
            new Lookup(task_master, source_reference, new String[] {"to_utc"},
            context).listen((to_utc) -> {
                if (to_utc instanceof Boolean) {
                    this.to_utc = (Boolean) to_utc;
                    if (interlock.decrementAndGet() == 0) {
                        task_master.slot(this);
                    }
                } else {
                    task_master.reportOtherError(source_reference,
                    "“to_utc” must be an Bool.");
                }
            });
            if (interlock.decrementAndGet() > 0) {
                return;
            }
        }
        result = makeTime(initial.withZoneSameInstant(to_utc
                          ? ZoneId.of("Z")
                          : ZoneId.systemDefault()));
        return;
    }
}
