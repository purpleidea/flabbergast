package flabbergast;

public interface ComputeOverride {
	public abstract Computation invoke(TaskMaster task_master,
			SourceReference reference, Context context, Frame self,
			Frame container, Computation original);
}
