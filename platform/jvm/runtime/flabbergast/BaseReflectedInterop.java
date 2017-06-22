package flabbergast;

import flabbergast.ReflectedFrame.Transform;
import java.util.Map;

public abstract class BaseReflectedInterop<R> extends InterlockedLookup {

    protected final Frame self;
    protected final Frame container;
    public BaseReflectedInterop(TaskMaster task_master, SourceReference source_reference,
                                Context context, Frame self, Frame container)  {
        super(task_master, source_reference, context);
        this.self = self;
        this.container = container;
    }

    protected abstract Map<String, Transform<R>> getAccessors();

    protected abstract R computeResult() throws Exception;

    @Override
    protected final void resolve() {
        try {
            result = ReflectedFrame.create(task_master, computeResult(), getAccessors());
        } catch (Exception e) {
            task_master.reportOtherError(source_reference, e.getMessage());
        }
    }
}
