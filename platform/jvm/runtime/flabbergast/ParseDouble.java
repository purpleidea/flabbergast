package flabbergast;

public class ParseDouble extends BaseMapFunctionInterop<String, Double> {
    public ParseDouble(TaskMaster task_master, SourceReference source_ref,
                       Context context, Frame self, Frame container) {
        super(Double.class, String.class,  task_master, source_ref, context, self, container);
    }
    @Override
    protected Double computeResult(String input)throws Exception {
        return Double.parseDouble(input);
    }
}
