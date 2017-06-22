package flabbergast;

public abstract class BaseFunctionInterop<R> extends InterlockedLookup {
    protected final Frame self;
    protected final Frame container;
    public BaseFunctionInterop(TaskMaster task_master, SourceReference source_reference,
                               Context context, Frame self, Frame container)  {
        super(task_master, source_reference, context);
        this.self = self;
        this.container = container;
    }

    protected abstract R computeResult() throws Exception;

    @Override
    protected final void resolve() {
        result = correctOutput(this::computeResult);
    }
}

