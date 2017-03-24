package flabbergast;

public interface ComputeOverride {
    public abstract Future invoke(TaskMaster task_master,
                                  SourceReference reference, Context context, Frame self,
                                  Frame container, Future original);
}
