package flabbergast;

import java.net.IDN;

public class Punycode extends Future {
    private InterlockedLookup interlock;
    private String input;
    private boolean encode;
    private boolean allow_unassigned;
    private boolean strict_ascii;

    private SourceReference source_reference;
    private Context context;

    public Punycode(TaskMaster task_master,
                    SourceReference source_ref, Context context, Frame self,
                    Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
    }
    @Override
    protected void run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.lookupStr(x->input = x, "arg");
            interlock.lookup(Boolean.class,
                             x->encode = x, "encode");
            interlock.lookup(Boolean.class, x-> allow_unassigned = x, "allow_unassigned");
            interlock.lookup(Boolean.class, x->strict_ascii = x, "strict_ascii");
        }
        if (!interlock.away()) return;
        try {
            int flags = (allow_unassigned ? IDN.ALLOW_UNASSIGNED  : 0) | (strict_ascii ? IDN.USE_STD3_ASCII_RULES : 0);
            result = new SimpleStringish(encode ? IDN.toASCII(input, flags) : IDN.toUnicode(input, flags));
            return;
        } catch (IllegalArgumentException e) {
            task_master.reportOtherError(source_reference, "Invalid punycode: " + e.getMessage());
        }
    }
}
