package flabbergast;

import java.util.Map;
import java.util.Map.Entry;
public abstract class BaseMapFunctionInterop<T, R> extends InterlockedLookup {
    private final Class<R> returnClass;
    private final  Class<T> clazz;
    protected final Frame self;
    protected final Frame container;
    private Map<String, T> input;

    public BaseMapFunctionInterop(Class<R> returnClass, Class<T> clazz, TaskMaster task_master, SourceReference source_reference,
                                  Context context, Frame self, Frame container)  {
        super(task_master, source_reference, context);
        this.returnClass = returnClass;
        this.clazz = clazz;
        this.self = self;
        this.container = container;
    }

    protected abstract R computeResult(T input)throws Exception;

    protected void setupExtra() {
    }
    @Override
    protected final void setup() {
        ListSink<T> args_lookup = findAll(clazz, x -> this.input = x);
        args_lookup.allowDefault(false, null);
        args_lookup.lookup("args");
        setupExtra();
    }
    @Override
    protected final void resolve() {
        MutableFrame output_frame = new MutableFrame(task_master, source_reference, context, self);
        for (Entry<String, T> entry : input.entrySet()) {
            output_frame.set(entry.getKey(), correctOutput(() -> computeResult(entry.getValue())));
        }
        result = output_frame;
    }
}

