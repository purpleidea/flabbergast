using System.Collections.Generic;
using System;
namespace Flabbergast {

public delegate Computation ComputeValue(Context context, Frame self, Frame container);
public delegate void ConsumeResult(Object result);

public abstract class Computation {
	private ConsumeResult consumer = null;
	private bool must_run = true;
	protected object result = null;

	protected abstract bool Run();

	internal void Compute() {
		if (must_run || Run()) {
			must_run = false;
			if (consumer != null) {
				consumer(result);
				consumer = null;
			}
		}
	}

	public void Notify(ConsumeResult new_consumer) {
		if (must_run) {
			if (consumer == null) {
				consumer = new_consumer;
			} else {
				consumer += new_consumer;
			}
		} else {
			new_consumer(result);
		}
	}
}

public abstract class TaskMaster {
	private Queue<Computation> computations = new Queue<Computation>();

	public TaskMaster() {}

	public void Run() {
		while(computations.Count > 0) {
			computations.Dequeue().Compute();
		}
	}

	public void Slot(Computation computation) {
		computations.Enqueue(computation);
	}

	public virtual void ReportLookupError(Lookup lookup) {
		ReportOtherError(lookup.SourceReference, String.Format("Undefined name “{0}”.", lookup.Name));
	}

	public abstract void ReportOtherError(SourceReference reference, string message);
}

/**
 * Do lookup by creating a grid of contexts where the value might reside and all the needed names.
 */
public class Lookup : Computation {
	private TaskMaster master;
	/**
	 * The name components in the lookup expression.
	 */
	private string[] names;

	public string Name {
			get { return string.Join(".", names); }
	}
	/**
	 * The dynamic programming grid. The first dimension is the context and the second is the name.
	 */
	private object[,] values;

	public SourceReference SourceReference {
		get;
		private set;
	}

	/**
	 * The current context in the grid being considered.
	 */
	private int frame = 0;
	/**
	 * The current name in the current context being considered.
	 */
	private int name = 0;
	/**
	 * This will be true when a value has been supplied by the callback to GetOrSubscribe.
	 */
	private bool got_value = false;
	/**
	 * This will be true when GetOrSubscribe did a subscribe and we were invoked later.
	 */
	private bool delayed = false;

	public Lookup(TaskMaster master, SourceReference source_ref, Context context, string[] names) {
		this.master = master;
		this.SourceReference = source_ref;
		this.names = names;
		/* Create  grid where the first entry is the frame under consideration. */
		var values = new object[context.Length, names.Length + 1];
		context.Fill((index, frame) => values[index, 0] = frame);
	}
	/**
	 * This is the callback used by GetOrSubscribe. It will be called when a value is available.
	 *
	 * If that was not immediately, then delayed will be true, so we slot outselves for further evaluation.
	 */
	private void ConsumeResult(object result) {
		values[frame, name++] = result;
		got_value = true;
		if (delayed) {
			master.Slot(this);
		}
	}
	protected override bool Run() {
		while (frame < values.GetLength(0)) {
			while (name < values.GetLength(1)) {
				// If we have reached the end of a list of names for the current frame, then we have an answer!
				if (name == values.GetLength(1) - 1) {
					result = values[frame, name];
					return true;
				}

				// If this is not a frame, but there are still more names, then this is an error.
				if (!(values[frame, name] is Frame)) {
					master.ReportLookupError(this);
				}

				got_value = false;
				delayed = false;
				// Otherwise, try to get the current value for the current name
				if (!((Frame)values[frame, name]).GetOrSubscribe(names[name], ConsumeResult)) {
					// The value isn't yet computed. Signal our callback that we want to be slotted.
					delayed = true;
					return false;
				}
				// If we got nothing, then it doesn't exist in the current frame. Try the next.
				if (!got_value) {
					name = 0;
					frame++;
					break;
				}
			}
		}
		// The name is undefined.
		master.ReportLookupError(this);
		return false;
	}
}
}
