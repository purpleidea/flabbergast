package flabbergast;
public abstract class BaseMapFunctionInterop<T, R> extends Future {
    private class MapFunction extends Future implements ConsumeResult {
        private final String arg_name;
        public MapFunction(TaskMaster task_master, String arg_name) {
            super(task_master);
            this.arg_name = arg_name;
        }

        public void consume(Object input_value)  {
            boolean correct;
            if (clazz.isInstance(input_value)) {
                correct = true;
            } else

                if (clazz.equals(String.class)) {
                    if (input_value instanceof Stringish) {
                        input_value = input_value.toString();
                        correct = true;
                    } else if (input_value instanceof Boolean) {
                        input_value = ((Boolean)input_value) ?  "True" : "False";
                        correct = true;
                    } else if (input_value instanceof Long) {
                        input_value = ((Long)input_value).toString();
                        correct = true;
                    } else if (input_value instanceof Double) {
                        input_value = ((Double)input_value).toString();
                        correct = true;
                    } else {
                        correct = false;
                    }
                } else if (clazz.equals(Double.class)) {
                    if (input_value instanceof Long) {
                        input_value = ((Long)input_value).doubleValue();
                        correct = true;
                    } else {
                        correct = false;
                    }
                } else {
                    correct = false;
                }


            if (correct) {
                try {
                    R output = computeResult((T) input_value);
                    if (output == null) {
                        result = Unit.NULL;
                    } else {
                        result = returnClass.equals(String.class) ? new SimpleStringish((String) output) : output;
                    }
                    wakeupListeners();
                } catch (Exception e) {
                    task_master.reportOtherError(source_reference, e.getMessage());
                }
            } else {
                task_master.reportOtherError(source_reference, String.format("“%s” has type %s but expected %s.", arg_name, SupportFunctions.nameForClass(input_value.getClass()), SupportFunctions.nameForClass(clazz)));
            }

        }
        @Override
        protected void run() {}
    }
    private InterlockedLookup interlock;
    private final Class<R> returnClass;
    private final  Class<T> clazz;
    protected final SourceReference source_reference;
    protected final Context context;
    protected final Frame self;
    protected final Frame container;
    private Frame input;

    public BaseMapFunctionInterop(Class<R> returnClass, Class<T> clazz, TaskMaster task_master, SourceReference source_ref,
                                  Context context, Frame self, Frame container)  {
        super(task_master);
        this.returnClass = returnClass;
        this.clazz = clazz;
        this.source_reference = source_ref;
        this.context = context;
        this.self = self;
        this.container = container;
    }

    protected abstract R computeResult(T input)throws Exception;

    protected void prepareLookup(InterlockedLookup interlock) {
    }
    @Override
    protected  void run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.lookup(Frame.class, x -> this.input = x, "args");
            prepareLookup(interlock);
        }
        if (!interlock.away()) return;
        MutableFrame output_frame = new MutableFrame(task_master, source_reference, context, self);
        for (String name : input) {
            MapFunction arg_value = new MapFunction(task_master, name);
            ComputeValue thunk = (task_master, source_reference, context, self, container) -> arg_value;
            output_frame.set(name, thunk);
            input.getOrSubscribe(name, arg_value);
        }
        result = output_frame;
    }
}

