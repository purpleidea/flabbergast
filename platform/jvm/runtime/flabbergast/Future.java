package flabbergast;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A generic computation to be worked on by the TaskMaster.
 */
public abstract class Future {

    /**
     * Apply an override to a normal computation resulting in another normal
     * computation.
     */
    public static ComputeValue performOverride(final String filename,
            final int start_line, final int start_column, final int end_line,
            final int end_column, final ComputeOverride wrapper,
            final ComputeValue original) {
        return new ComputeValue() {

            @Override
            public Future invoke(TaskMaster task_master,
                                 SourceReference reference, Context context, Frame self,
                                 Frame container) {
                SourceReference inner_reference = new BasicSourceReference(
                    "used by override", filename, start_line, start_column,
                    end_line, end_column, reference);
                if (original == null) {
                    return new FailureFuture(task_master, inner_reference,
                                             "override of non-existant attribute");
                }

                return wrapper.invoke(task_master, reference, context, self,
                                      container, original.invoke(task_master,
                                              inner_reference, context, self, container));
            }

        };
    }

    /**
     * The delegate(s) to be invoked when the computation is complete.
     */
    private ArrayList<ConsumeResult> consumer = new ArrayList<ConsumeResult>();
    /**
     * The return value of the computation.
     *
     * This should be assigned by the subclass.
     */
    protected Object result = null;

    protected final TaskMaster task_master;

    private boolean virgin = true;

    private final Lock ex = new ReentrantLock();

    public Future(TaskMaster task_master) {
        this.task_master = task_master;
    }

    /**
     * Called by the TaskMaster to start or continue computation.
     */
    void compute() {
        if (result == null) {
            run();
            if (result == null) {
                return;
            }
            wakeupListeners();
        }
    }

    /**
     * Attach a callback when the computation is complete. If already complete,
     * the callback is immediately invoked.
     */
    public void listen(ConsumeResult new_consumer) {
        listen(new_consumer, true);
    }

    public void listen(ConsumeResult new_consumer, boolean needs_slot) {
        ex.lock();
        if (result == null) {
            consumer.add(new_consumer);
            if (needs_slot) {
                slotHelper();
            }
            ex.unlock();
        } else {
            ex.unlock();
            new_consumer.consume(result);
        }
    }

    public void listenDelayed(ConsumeResult new_consumer) {
        listen(new_consumer, false);
    }

    /**
     * The method that will be invoked when the result is needed.
     */
    protected abstract void run();

    public void slot() {
        ex.lock();
        if (result == null) {
            slotHelper();
        }
        ex.unlock();
    }

    private void slotHelper() {
        if (virgin && task_master != null) {
            virgin = false;
            task_master.slot(this);
        }
    }

    protected void wakeupListeners() {
        if (result == null) {
            throw new UnsupportedOperationException();
        }
        ex.lock();
        ArrayList<ConsumeResult> consumer_copy = consumer;
        consumer = null;
        ex.unlock();

        if (consumer_copy == null) {
            return;
        }

        for (ConsumeResult cr : consumer_copy) {
            cr.consume(result);
        }
    }
}
