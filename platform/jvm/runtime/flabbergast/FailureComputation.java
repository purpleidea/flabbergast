package flabbergast;

public class FailureComputation extends Computation {
	private TaskMaster task_master;
	private String message;
	private SourceReference source_reference;
	public FailureComputation(TaskMaster task_master, SourceReference reference, String message) {
		this.task_master = task_master;
		this.source_reference = reference;
		this.message = message;
	}

	@Override
	protected void run() {
		task_master.reportOtherError(source_reference, message);
	}
}
