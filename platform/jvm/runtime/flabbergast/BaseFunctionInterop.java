package flabbergast;

public abstract class BaseFunctionInterop<R> extends Future {
    private InterlockedLookup interlock;
    protected final SourceReference source_reference;
    protected final Context context;
    protected final Frame self;
    protected final Frame container;
    private final boolean isString;
    public BaseFunctionInterop(Class<R> clazz, TaskMaster task_master, SourceReference source_ref,
                               Context context, Frame self, Frame container)  {
        super(task_master);
        isString = clazz.equals(String.class);
        this.source_reference = source_ref;
        this.context = context;
        this.self = self;
        this.container = container;
    }

    protected abstract R computeResult()throws Exception;

    protected abstract void prepareLookup(InterlockedLookup interlock);

    @Override
    protected void run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            prepareLookup(interlock);
        }
        if (!interlock.away()) return;
        try {
            R output = computeResult();
            if (output == null) {
                result = Unit.NULL;
            } else {
                result = isString ? new SimpleStringish((String) output) : output;
            }
        } catch (Exception e) {
            task_master.reportOtherError(source_reference, e.getMessage());
        }
    }
}

