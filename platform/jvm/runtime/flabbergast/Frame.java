package flabbergast;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A Frame in the Flabbergast language.
 */
public class Frame implements Iterable<String> {

	public static Frame through(TaskMaster task_master, long id,
			SourceReference source_ref, long start, long end, Context context,
			Frame container) {
		Frame result = new Frame(task_master, id, source_ref, context,
				container);
		if (end < start)
			return result;
		for (long it = 0; it <= (end - start); it++) {
			result.set(TaskMaster.ordinalNameStr(it + 1), start + it);
		}
		return result;
	}

	private TreeMap<String, Object> attributes = new TreeMap<String, Object>();
	private Frame container;
	private Context context;
	private Stringish id;
	private SourceReference source_reference;
	private TaskMaster task_master;

	private ArrayList<Computation> unslotted = new ArrayList<Computation>();

	public Frame(TaskMaster task_master, long id, SourceReference source_ref,
			Context context, Frame container) {
		this.task_master = task_master;
		this.source_reference = source_ref;
		this.context = Context.prepend(this, context);
		this.container = container == null ? this : container;
		this.id = TaskMaster.ordinalName(id);
	}

	public int count() {
		return attributes.size();
	}

	/**
	 * Access the functions in the frames. Frames should not be mutated, but
	 * this policy is not enforced by this class; it must be done in the calling
	 * code.
	 */
	public Object get(String name) {
		return attributes.containsKey(name) ? attributes.get(name) : null;
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

		Entry<String, Object> attribute_entry = attributes.ceilingEntry(name);
		if (attribute_entry == null || !attribute_entry.getKey().equals(name)) {
			return false;
		}
		if (attribute_entry.getValue() instanceof Computation) {
			((Computation) attribute_entry.getValue()).listen(consumer);
		} else {

			consumer.consume(attribute_entry.getValue());
		}
		return true;
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
		return attributes.containsKey(name);
	}

	public boolean has(Stringish name) {
		return has(name.toString());
	}

	@Override
	public Iterator<String> iterator() {
		return attributes.keySet().iterator();
	}

	public Stringish renderTrace(Stringish prefix) {
		StringWriter writer = new StringWriter();
		HashSet<SourceReference> seen = new HashSet<SourceReference>();
		try {
			source_reference.write(writer, prefix.toString(), seen);
		} catch (IOException e) {
			// This should be impossible with StringWriter.
		}
		return new SimpleStringish(writer.toString());
	}

	public void set(final String name, Object value) {
		if (value == null) {
			return;
		}
		if (attributes.containsKey(name)) {
			throw new IllegalStateException("Redefinition of attribute " + name
					+ ".");
		}
		if (value instanceof ComputeValue) {
			Computation computation = ((ComputeValue) value).invoke(
					task_master, source_reference, context, this, container);
			attributes.put(name, computation);
			/*
			 * When this computation has completed, replace its value in the
			 * frame.
			 */
			computation.listen(new ConsumeResult() {
				@Override
				public void consume(Object result) {
					attributes.put(name, result);
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
