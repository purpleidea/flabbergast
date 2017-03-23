package flabbergast;

public class ParseInt extends Future {

    private InterlockedLookup interlock;
    private String input;
    private int radix;

    private SourceReference source_reference;
    private Context context;
    private Frame container;

    public ParseInt(TaskMaster task_master, SourceReference source_ref,
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
            interlock.lookup(Long.class, x->radix = x.intValue(), "radix");
        }

        if (!interlock.away()) return;
        if (radix < Character.MIN_RADIX) {
            task_master.reportOtherError(source_reference,
                                         String.format(
                                             "Radix %s must be at least %s.",
                                             radix, Character.MIN_RADIX));
            return;
        } else if (radix > Character.MAX_RADIX) {
            task_master.reportOtherError(source_reference,
                                         String.format(
                                             "Radix %s must be at most %s.",
                                             radix, Character.MAX_RADIX));
            return;
        }
        try {
            result = Long.parseLong(input, radix);
        } catch (NumberFormatException e) {
            task_master.reportOtherError(source_reference,
                                         String.format("Invalid integer “%s”.", input));
        }
    }
}
