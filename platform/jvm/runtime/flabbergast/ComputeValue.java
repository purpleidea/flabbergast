package flabbergast;

public interface ComputeValue {
    public Computation invoke(TaskMaster task_master,
                              SourceReference source_reference, Context context, Frame self,
                              Frame container);
}
