package flabbergast;

/**
 * Holds a value for inclusion of a pre-computed value in a template.
 */
public class Precomputation extends Future implements ComputeValue {

    public Precomputation(Object result) {
        super(null);
        this.result = result;
    }

    @Override
    public Future invoke(TaskMaster task_master,
                              SourceReference source_reference, Context context, Frame self,
                              Frame container) {
        return this;
    }

    @Override
    protected void run() {
    }
}
