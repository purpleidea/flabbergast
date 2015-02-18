using System.Collections.Generic;
using System;
namespace Flabbergast {

public delegate Computation ComputeValue(TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container);
public delegate Computation ComputeOverride(TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container, Computation original);

/**
 * Delegate for the callback from a computation.
 */
public delegate void ConsumeResult(Object result);

/**
 * A generic computation to be worked on by the TaskMaster.
 */
public abstract class Computation {

	/**
	 * Apply an override to a normal computation resulting in another normal computation.
	 */
	public static ComputeValue PerformOverride(string filename, int start_line, int start_column, int end_line, int end_column, ComputeOverride wrapper, ComputeValue original) {
		return (task_master, reference, context, self, container) => wrapper(task_master, reference, context, self, container, original(task_master, new SourceReference("used by override", filename, start_line, start_column, end_line, end_column, reference), context, self, container));
	}

	/**
	 * The delegate(s) to be invoked when the computation is complete.
	 */
	private ConsumeResult consumer = null;
	/**
	 * True until the computation has been fully completed.
	 */
	private bool must_run = true;
	/**
	 * The return value of the computation.
	 *
	 * This should be assigned by the subclass.
	 */
	protected object result = null;

	/**
	 * Called by the TaskMaster to start or continue computation.
	 */
	internal void Compute() {
		if (must_run || Run()) {
			must_run = false;
			if (consumer != null) {
				consumer(result);
				consumer = null;
			}
		}
	}

	/**
	 * Attach a callback when the computation is complete. If already complete,
	 * the callback is immediately invoked.
	 */
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

	/**
	 * The method that will be invoked when the result is needed. If the method
	 * returns true, the computation is finished. Otherwise, it is assumed that
	 * the computation needs to wait another value.
	 */
	protected abstract bool Run();
}

public interface UriHandler {
	bool ResolveUri(SourceReference reference, string uri, ConsumeResult consume_result);
}

/**
 * Scheduler for computations.
 */
public abstract class TaskMaster {
	private Queue<Computation> computations = new Queue<Computation>();
	private List<UriHandler> handlers = new List<UriHandler>();
	private Dictionary<string, object> external_cache = new Dictionary<string, object>();

	private long next_id = 0;

	public static Stringish OrdinalName(long id) {
		return new SimpleStringish(OrdinalNameStr(id));
	}
	public static string OrdinalNameStr(long id) {
		var len = (sizeof (long) * 8 * Math.Log (2) / Math.Log (62)) + 1;
		var id_str = new System.Text.StringBuilder();
		if (id < 0) {
			id_str.Append('e');
			id = long.MaxValue + id;
		} else {
			id_str.Append('f');
		}
		for (var it = len; it > 0; it--) {
			var digit = (char) (id % 62);
			id = id / 62;
			if (digit < 10) {
				id_str.Append('0' + digit);
			} else if (digit < 36) {
				id_str.Append('A' + (digit - 10));
			} else {
				id_str.Append('a' + (digit - 36));
			}
		}
		return id_str.ToString();
	}

	public TaskMaster() {}

	public void AddUriHandler(UriHandler handler) {
		handlers.Add(handler);
	}

	public virtual void GetExternal(SourceReference reference, string uri, ConsumeResult target) {
		if (external_cache.ContainsKey(uri)) {
			target(external_cache[uri]);
		}
		foreach (var handler in handlers) {
			if (handler.ResolveUri(reference, uri, (result) => { external_cache[uri] = result; target(result); })) {
				return;
			}
		}
		ReportOtherError(reference, String.Format("The URI “{0}” could not be resolved.", uri));
	}

	public long NextId() {
		return System.Threading.Interlocked.Increment(ref next_id);
	}

	/**
	 * Perform computations until the Flabbergast program is complete or deadlocked.
	 */
	public void Run() {
		while(computations.Count > 0) {
			computations.Dequeue().Compute();
		}
	}

	/**
	 * Report an error during lookup.
	 */
	public virtual void ReportLookupError(Lookup lookup) {
		ReportOtherError(lookup.SourceReference, String.Format("Undefined name “{0}”.", lookup.Name));
	}

	/**
	 * Report an error during execution of the program.
	 */
	public abstract void ReportOtherError(SourceReference reference, string message);

	/**
	 * Add a computation to be executed.
	 */
	public void Slot(Computation computation) {
		computations.Enqueue(computation);
	}
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

	public Lookup(TaskMaster master, SourceReference source_ref, string[] names, Context context) {
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
