package flabbergast;

import javax.xml.bind.DatatypeConverter;

public class FromBase64 extends Computation {

    private InterlockedLookup interlock;
    private String input;

    private SourceReference source_reference;
    private Context context;
    private Frame container;

    public FromBase64(TaskMaster task_master, SourceReference source_ref,
                      Context context, Frame self, Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
        this.container = self;
    }

    @Override
    protected void run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.lookupStr(x->input = x, "arg");
        }
        if (!interlock.away()) return;

        try {
            result = DatatypeConverter.parseBase64Binary(input);
        } catch (IllegalArgumentException e) {
            task_master.reportOtherError(source_reference,
                                         e.getMessage());
        }
    }
}
