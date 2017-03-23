package flabbergast;

public interface ComputeValue {
    public Future invoke(TaskMaster task_master,
                              SourceReference source_reference, Context context, Frame self,
                              Frame container);
}
