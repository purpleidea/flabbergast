package flabbergast;

import java.util.ArrayList;

/**
 * A generic computation to be worked on by the TaskMaster.
 */
public abstract class Computation {

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
			public Computation invoke(TaskMaster task_master,
					SourceReference reference, Context context, Frame self,
					Frame container) {
				return wrapper.invoke(task_master, reference, context, self,
						container, original.invoke(task_master,
								new SourceReference("used by override",
										filename, start_line, start_column,
										end_line, end_column, reference),
								context, self, container));

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

	public Computation() {
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
		if (result == null) {
			consumer.add(new_consumer);
		} else {
			new_consumer.consume(result);
		}
	}

	/**
	 * The method that will be invoked when the result is needed. If the method
	 * returns true, the computation is finished. Otherwise, it is assumed that
	 * the computation needs to wait another value.
	 */
	protected abstract void run();

	protected void wakeupListeners() {
		for (ConsumeResult cr : consumer)
			cr.consume(result);
		consumer.clear();
	}
}