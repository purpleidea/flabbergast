package flabbergast;

public class ParseDouble extends Computation {

    private InterlockedLookup interlock;
    private String input;

    private SourceReference source_reference;
    private Context context;

    public ParseDouble(TaskMaster task_master, SourceReference source_ref,
                       Context context, Frame self, Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
    }

    @Override
    protected void run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.lookupStr(x->input = x, "arg");
        }
        if (!interlock.away()) return;

        try {
            result = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            task_master.reportOtherError(source_reference,
                                         String.format("Invalid integer “%s”.", input));
        }
    }
}
