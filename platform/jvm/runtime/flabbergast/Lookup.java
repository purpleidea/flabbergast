package flabbergast;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Do lookup by creating a grid of contexts where the value might reside and all
 * the needed names.
 */
public class Lookup extends Computation implements ConsumeResult {
	/**
	 * The current context in the grid being considered.
	 */
	private int frame = 0;
	private TaskMaster master;

	/**
	 * The current name in the current context being considered.
	 */
	private int name = 0;

	/**
	 * The name components in the lookup expression.
	 */
	private String[] names;
	private AtomicInteger outstanding = new AtomicInteger();
	private SourceReference source_reference;

	/**
	 * The dynamic programming grid. The first dimension is the context and the
	 * second is the name.
	 */
	private Object[][] values;

	public Lookup(TaskMaster master, SourceReference source_ref,
			String[] names, Context context) {
		this.master = master;
		this.source_reference = source_ref;
		this.names = names;
		/* Create grid where the first entry is the frame under consideration. */
		values = new Object[context.getLength()][];
		int index = 0;
		for (Frame frame : context) {
			values[index] = new Object[names.length + 1];
			values[index][0] = frame;
			index++;
		}
	}

	/**
	 * This is the callback used by GetOrSubscribe. It will be called when a
	 * value is available.
	 * 
	 * If that was not immediately, then delayed will be true, so we slot
	 * ourselves for further evaluation.
	 */
	@Override
	public void consume(Object return_value) {
		values[frame][++name] = return_value;
		if (outstanding.decrementAndGet() == 0) {
			master.slot(this);
		}
	}

	public Frame get(int name, int frame) {
		return (Frame) values[frame][name];
	}

	public int getFrameCount() {
		return values.length;
	}

	public String getName() {
		StringBuilder sb = new StringBuilder();
		for (int n = 0; n < names.length; n++) {
			if (n > 0)
				sb.append(".");
			sb.append(names[n]);
		}
		return sb.toString();
	}

	public String getName(int index) {
		return names[index];
	}

	public int getNameCount() {
		return names.length;
	}

	public SourceReference getSourceReference() {
		return source_reference;
	}

	@Override
	protected void run() {
		while (frame < values.length && name < values[0].length) {
			// If we have reached the end of a list of names for the current
			// frame, then we have an answer!
			if (name == values[0].length - 1) {

				result = values[frame][name];
				return;
			}

			// If this is not a frame, but there are still more names, then this
			// is an error.
			if (!(values[frame][name] instanceof Frame)) {
				master.reportLookupError(this, values[frame][name].getClass());
				return;
			}

			// Otherwise, try to get the current value for the current name
			outstanding.set(2);
			if (((Frame) values[frame][name]).getOrSubscribe(names[name], this)) {
				if (outstanding.decrementAndGet() > 0) {
					return;
				}
			} else {
				name = 0;
				frame++;
			}
		}
		// The name is undefined.
		master.reportLookupError(this, null);
		return;
	}

}
