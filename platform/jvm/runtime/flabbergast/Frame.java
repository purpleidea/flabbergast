package flabbergast;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;

/**
 * A Frame in the Flabbergast language.
 */
public abstract class Frame implements Iterable<String> {

	public static Frame through(TaskMaster task_master,
			SourceReference source_ref, long start, long end, Context context,
			Frame container) {
		MutableFrame result = new MutableFrame(task_master, source_ref,
				context, container);
		if (end < start)
			return result;
		for (long it = 0; it <= (end - start); it++) {
			result.set(TaskMaster.ordinalNameStr(it + 1), start + it);
		}
		return result;
	}

	private final Frame container;
	private final Context context;
	private final Stringish id;
	private final SourceReference source_reference;

	public Frame(TaskMaster task_master, SourceReference source_ref,
			Context context, Frame container) {
		this(TaskMaster.ordinalName(task_master.nextId()), source_ref, context,
				container);
	}

	public Frame(String id, SourceReference source_ref, Context context,
			Frame container) {
		this(new SimpleStringish(id), source_ref, context, container);
	}

	public Frame(Stringish id, SourceReference source_ref, Context context,
			Frame container) {
		this.source_reference = source_ref;
		this.context = Context.prepend(this, context);
		this.container = container == null ? this : container;
		this.id = id;
	}

	public abstract int count();

	/**
	 * Access the functions in the frames. Frames should not be mutated, but
	 * this policy is not enforced by this class; it must be done in the calling
	 * code.
	 */
	public abstract Object get(String name);

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
		Object result = get(name);
		if (result == null) {
			return false;
		}
		if (result instanceof Computation) {
			((Computation) result).listen(consumer);
		} else {
			consumer.consume(result);
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
	public abstract boolean has(String name);

	public boolean has(Stringish name) {
		return has(name.toString());
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
}
