package flabbergast;

import java.util.HashMap;
import java.util.Map;

public class StringToCodepoints extends Computation {
    private InterlockedLookup interlock;
    private String input;

    private SourceReference source_reference;
    private Context context;
    private Frame container;

    public StringToCodepoints(TaskMaster task_master,
                              SourceReference source_ref, Context context, Frame self,
                              Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
        this.container = self;
    }

    @Override
    protected void run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.lookupStr(x-> input = x, "arg");
        }
        if (!interlock.away()) return;
        MutableFrame frame = new MutableFrame(task_master, source_reference,
                                              context, container);
        for (int it = 0; it < input.length(); it++) {
            frame.set(TaskMaster.ordinalNameStr(it + 1),
                      (long) input.codePointAt(it));
        }
        result = frame;
    }
}
