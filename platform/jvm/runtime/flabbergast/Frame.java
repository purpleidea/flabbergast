package flabbergast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A Frame in the Flabbergast language.
 */
public class Frame implements AttributeNames {

	public static Frame Through(TaskMaster task_master, long id,
			SourceReference source_ref, long start, long end, Context context,
			Frame container) {
		Frame result = new Frame(task_master, id, source_ref, context,
				container);
		if (end < start)
			return result;
		for (long it = 0; it <= (end - start); it++) {
			result.set(TaskMaster.OrdinalNameStr(it + 1), start + it);
		}
		return result;
	}

	private Map<String, Object> attributes = new HashMap<String, Object>();
	private Frame container;
	private Context context;
	private Stringish id;
	private Map<String, Computation> pending = new HashMap<String, Computation>();
	private SourceReference source_reference;
	private TaskMaster task_master;

	private ArrayList<Computation> unslotted = new ArrayList<Computation>();

	public Frame(TaskMaster task_master, long id, SourceReference source_ref,
			Context context, Frame container) {
		this.task_master = task_master;
		this.source_reference = source_ref;
		this.context = Context.Prepend(this, context);
		this.container = container;
		this.id = TaskMaster.OrdinalName(id);
	}

	/**
	 * Access the functions in the frames. Frames should not be mutated, but
	 * this policy is not enforced by this class; it must be done in the calling
	 * code.
	 */
	public Object get(String name) {
		return attributes.containsKey(name) ? attributes.get(name) : null;
	}

	@Override
	public Iterator<String> getAttributeNames() {
		return new ConcatIterator<String>(attributes.keySet().iterator(),
				pending.keySet().iterator());
	}

	/**
	 * The containing frame, or null for file-level frames.
	 */
	public Frame getContainer() {
		return container;
	}

	/**
	 * The lookup context when this frame was created and any of its ancestors.
	 */
	public Context getContext() {
		return context;
	}

	public Stringish getId() {
		return id;
	}

	/**
	 * Access a value if available, or be notified upon completion. Returns:
	 * true if the value was available, false if the caller should wait to be
	 * reinvoked.
	 */
	boolean getOrSubscribe(String name, ConsumeResult consumer) {
		// If this frame is being looked at, then all its pending attributes
		// should
		// be slotted.
		slot();

		if (pending.containsKey(name)) {
			pending.get(name).listen(consumer);
			return true;
		}
		if (attributes.containsKey(name)) {
			consumer.consume(attributes.get(name));
			return true;
		}
		return false;
	}

	/**
	 * The stack trace when this frame was created.
	 */
	public SourceReference getSourceReference() {
		return source_reference;
	}

	/**
	 * Check if an attribute name is present in the frame.
	 */
	public boolean has(String name) {
		boolean is_pending = pending.containsKey(name);
		return is_pending || attributes.containsKey(name);
	}

	public void set(final String name, Object value) {
		if (value == null) {
			return;
		}
		if (pending.containsKey(name) || attributes.containsKey(name)) {
			throw new IllegalStateException("Redefinition of attribute " + name
					+ ".");
		}
		if (value instanceof ComputeValue) {
			Computation computation = ((ComputeValue) value).invoke(
					task_master, source_reference, context, this, container);
			pending.put(name, computation);
			/*
			 * When this computation has completed, replace its value in the
			 * frame.
			 */
			computation.listen(new ConsumeResult() {
				@Override
				public void consume(Object result) {
					attributes.put(name, result);
					pending.remove(name);
				}
			});
			/*
			 * If the value is a computation, it cannot be slotted for execution
			 * since it might depend on lookups that reference this frame.
			 * Therefore, put it in a queue for later activation.
			 */
			unslotted.add(computation);
		} else {
			if (value instanceof Frame) {
				/*
				 * If the value added is a frame, it might be in a complicated
				 * slotting arrangement. The safest thing to do is to steal its
				 * unslotted children and slot them when we are slotted (or
				 * absorbed into another frame.
				 */

				Frame other = (Frame) value;
				unslotted.addAll(other.unslotted);
				other.unslotted = unslotted;
			}
			attributes.put(name, value);
		}
	}

	/**
	 * Trigger any unfinished computations contained in this frame to be
	 * executed.
	 * 
	 * When a frame is being filled, unfinished computations may be added. They
	 * cannot be started immediately, since the frame may still have members to
	 * be added and those changes will be visible to the lookup environments of
	 * those computations. Only when a frame is “returned” can the computations
	 * be started. This should be called before returning to trigger
	 * computation.
	 */
	public void slot() {
		for (Computation computation : unslotted) {
			task_master.slot(computation);
		}
		unslotted.clear();
	}
}
