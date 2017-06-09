package flabbergast;

public class ParseInt extends BaseMapFunctionInterop<String, Long> {
    private int radix;

    public ParseInt(TaskMaster task_master, SourceReference source_ref,
                    Context context, Frame self, Frame container) {
        super(Long.class , String.class, task_master, source_ref, context, self, container);
    }

    @Override
    protected Long computeResult(String input)throws Exception {
        if (radix < Character.MIN_RADIX) {
            throw new IllegalArgumentException(String.format(
                                                   "Radix %s must be at least %s.",
                                                   radix, Character.MIN_RADIX));
        } else if (radix > Character.MAX_RADIX) {
            throw new IllegalArgumentException(String.format(
                                                   "Radix %s must be at most %s.",
                                                   radix, Character.MAX_RADIX));
        }
        return Long.parseLong(input, radix);

    }    protected void prepareLookup(InterlockedLookup interlock) {
        interlock.lookup(Long.class, x->radix = x.intValue(), "radix");
    }

}
