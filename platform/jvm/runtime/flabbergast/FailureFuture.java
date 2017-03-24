package flabbergast;

public class FailureFuture extends Future {
    private String message;
    private SourceReference source_reference;
    public FailureFuture(TaskMaster task_master,
                         SourceReference reference, String message) {
        super(task_master);
        this.source_reference = reference;
        this.message = message;
    }

    @Override
    protected void run() {
        task_master.reportOtherError(source_reference, message);
    }
}
