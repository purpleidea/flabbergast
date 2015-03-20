using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Threading;

namespace Flabbergast {
	public delegate Computation ComputeValue(
		TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container);

	public delegate Computation ComputeOverride(
		TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container,
		Computation original);

/**
 * Delegate for the callback from a computation.
 */

	public delegate void ConsumeResult(Object result);

/**
 * A generic computation to be worked on by the TaskMaster.
 */

	public abstract class Computation {
		/**
	 * The delegate(s) to be invoked when the computation is complete.
	 */
		private ConsumeResult consumer;
		/**
	 * The return value of the computation.
	 *
	 * This should be assigned by the subclass.
	 */
		protected object result;
		/**
	 * Called by the TaskMaster to start or continue computation.
	 */

		internal void Compute() {
			if (result == null && Run()) {
				if (result == null) {
					throw new InvalidOperationException("The computation " + GetType() +
					                                    " did not return a value. This is a bug.");
				}
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
			if (result == null) {
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
	 * Apply an override to a normal computation resulting in another normal computation.
	 */

		public static ComputeValue PerformOverride(string filename, int start_line, int start_column, int end_line,
			int end_column, ComputeOverride wrapper, ComputeValue original) {
			return
				(task_master, reference, context, self, container) =>
					wrapper(task_master, reference, context, self, container,
						original(task_master,
							new SourceReference("used by override", filename, start_line, start_column, end_line,
								end_column, reference), context, self, container));
		}

		/**
	 * The method that will be invoked when the result is needed. If the method
	 * returns true, the computation is finished. Otherwise, it is assumed that
	 * the computation needs to wait another value.
	 */
		protected abstract bool Run();
	}

	public interface UriHandler {
		string UriName { get; }
		Type ResolveUri(string uri, out bool stop);
	}

/**
 * Scheduler for computations.
 */

	public abstract class TaskMaster : IEnumerable<Computation> {
		private readonly Queue<Computation> computations = new Queue<Computation>();
		private readonly Dictionary<string, Computation> external_cache = new Dictionary<string, Computation>();
		private readonly List<UriHandler> handlers = new List<UriHandler>();
		/**
	 * These are computations that have not completed.
	 */
		private readonly Dictionary<Computation, bool> inflight = new Dictionary<Computation, bool>();
		private long next_id;

		public IEnumerator<Computation> GetEnumerator() {
			return inflight.Keys.GetEnumerator();
		}

		IEnumerator IEnumerable.GetEnumerator() {
			return GetEnumerator();
		}

		public void AddUriHandler(UriHandler handler) {
			handlers.Add(handler);
		}

		private static char[] CreateOrdinalSymbols() {
			var array = new char[62];
			for (var it = 0; it < 10; it++) {
				array[it] = (char) ('0' + it);
			}
			for (var it = 0; it < 26; it++) {
				array[it + 10] = (char) ('A' + it);
				array[it + 36] = (char) ('a' + it);
			}
			return array;
		}

		public virtual void GetExternal(string uri, ConsumeResult target) {
			if (external_cache.ContainsKey(uri)) {
				external_cache[uri].Notify(target);
				return;
			}
			if (uri.StartsWith("lib:")) {
				if (uri.Length < 5) {
					ReportExternalError(uri);
					return;
				}
				for (var it = 5; it < uri.Length; it++) {
					if (uri[it] != '/' && !char.IsLetterOrDigit(uri[it])) {
						ReportExternalError(uri);
						return;
					}
				}
			}

			foreach (var handler in handlers) {
				bool stop;
				var t = handler.ResolveUri(uri, out stop);
				if (stop) {
					ReportExternalError(uri);
					return;
				}
				if (t == null) {
					continue;
				}
				if (!typeof(Computation).IsAssignableFrom(t)) {
					throw new InvalidCastException(String.Format(
						"Class {0} for URI {1} from {2} is not a computation.", t, uri, handler.UriName));
				}
				var computation = (Computation) Activator.CreateInstance(t, this);
				external_cache[uri] = computation;
				computation.Notify(target);
				Slot(computation);
				return;
			}
			ReportExternalError(uri);
		}

		public long NextId() {
			return Interlocked.Increment(ref next_id);
		}

		public static Stringish OrdinalName(long id) {
			return new SimpleStringish(OrdinalNameStr(id));
		}

		public static string OrdinalNameStr(long id) {
			var id_str = new char[(int) (sizeof (long) * 8 * Math.Log(2, symbols.Length)) + 1];
			if (id < 0) {
				id_str[0] = 'e';
				id = long.MaxValue + id;
			} else {
				id_str[0] = 'f';
			}
			for (var it = id_str.Length - 1; it > 0; it--) {
				id_str[it] = symbols[id % symbols.Length];
				id = id / symbols.Length;
			}
			return new string(id_str);
		}

		public abstract void ReportExternalError(string uri);
		/**
	 * Report an error during lookup.
	 */

		public virtual void ReportLookupError(Lookup lookup, Type fail_type) {
			if (fail_type == null) {
				ReportOtherError(lookup.SourceReference,
					String.Format("Undefined name “{0}”. Lookup was as follows:", lookup.Name));
			} else {
				ReportOtherError(lookup.SourceReference,
					String.Format("Non-frame type {1} while resolving name “{0}”. Lookup was as follows:", lookup.Name,
						fail_type));
			}
		}

		/**
	 * Report an error during execution of the program.
	 */
		public abstract void ReportOtherError(SourceReference reference, string message);
		/**
	 * Perform computations until the Flabbergast program is complete or deadlocked.
	 */

		public void Run() {
			while (computations.Count > 0) {
				var task = computations.Dequeue();
				task.Compute();
			}
		}

		/**
	 * Add a computation to be executed.
	 */

		public void Slot(Computation computation) {
			if (!inflight.ContainsKey(computation)) {
				computation.Notify(x => inflight.Remove(computation));
			}
			computations.Enqueue(computation);
		}

		public bool VerifySymbol(SourceReference source_reference, Stringish strish) {
			var str = strish.ToString();
			if (str.Length < 1) {
				ReportOtherError(source_reference, "An attribute name cannot be empty.");
				return false;
			}
			switch (Char.GetUnicodeCategory(str[0])) {
				case UnicodeCategory.LowercaseLetter:
				case UnicodeCategory.OtherLetter:
					break;
				default:
					ReportOtherError(source_reference,
						String.Format("The name “{0}” is unbecoming of an attribute; it cannot start with “{1}”.", str,
							str[0]));
					return false;
			}
			for (var it = 1; it < str.Length; it++) {
				if (str[it] == '_') {
					continue;
				}
				switch (Char.GetUnicodeCategory(str[it])) {
					case UnicodeCategory.DecimalDigitNumber:
					case UnicodeCategory.LetterNumber:
					case UnicodeCategory.LowercaseLetter:
					case UnicodeCategory.OtherLetter:
					case UnicodeCategory.OtherNumber:
					case UnicodeCategory.TitlecaseLetter:
					case UnicodeCategory.UppercaseLetter:
						continue;
					default:
						ReportOtherError(source_reference,
							String.Format("The name “{0}” is unbecoming of an attribute; it cannot contain “{1}”.", str,
								str[it]));
						return false;
				}
			}
			return true;
		}

		private static readonly char[] symbols = CreateOrdinalSymbols();
	}

/**
 * Do lookup by creating a grid of contexts where the value might reside and all the needed names.
 */

	public class Lookup : Computation {
		public int FrameCount {
			get { return values.GetLength(0); }
		}

		public Frame this[int name, int frame] {
			get { return values[frame, name] as Frame; }
		}

		public string Name {
			get { return string.Join(".", names); }
		}

		public int NameCount {
			get { return names.Length; }
		}

		public SourceReference SourceReference { get; private set; }
		/**
	 * The current context in the grid being considered.
	 */
		private int frame;
		private int interlock;
		private readonly TaskMaster master;
		/**
	 * The current name in the current context being considered.
	 */
		private int name;
		/**
	 * The name components in the lookup expression.
	 */
		private readonly string[] names;
		/**
	 * The dynamic programming grid. The first dimension is the context and the second is the name.
	 */
		private readonly object[,] values;

		public Lookup(TaskMaster master, SourceReference source_ref, string[] names, Context context) {
			this.master = master;
			SourceReference = source_ref;
			this.names = names;

			/* Create  grid where the first entry is the frame under consideration. */
			values = new object[context.Length, names.Length + 1];
			var index = 0;
			foreach (var frame in context.Fill()) {
				values[index++, 0] = frame;
			}
		}

		/**
	 * This is the callback used by GetOrSubscribe. It will be called when a value is available.
	 *
	 * If that was not immediately, then delayed will be true, so we slot outselves for further evaluation.
	 */

		private void ConsumeResult(object return_value) {
			values[frame, ++name] = return_value;
			if (Interlocked.Decrement(ref interlock) == 0) {
				master.Slot(this);
			}
		}

		public string GetName(int index) {
			return names[index];
		}

		protected override bool Run() {
			while (frame < values.GetLength(0) && name < values.GetLength(1)) {
				// If we have reached the end of a list of names for the current frame, then we have an answer!
				if (name == values.GetLength(1) - 1) {
					result = values[frame, name];
					return true;
				}

				// If this is not a frame, but there are still more names, then this is an error.
				if (!(values[frame, name] is Frame)) {
					master.ReportLookupError(this, values[frame, name].GetType());
					return false;
				}

				// Otherwise, try to get the current value for the current name
				interlock = 2;
				if (((Frame) values[frame, name]).GetOrSubscribe(names[name], ConsumeResult)) {
					if (Interlocked.Decrement(ref interlock) > 0) {
						return false;
					}
				} else {
					name = 0;
					frame++;
				}
			}
			// The name is undefined.
			master.ReportLookupError(this, null);
			return false;
		}
	}
}