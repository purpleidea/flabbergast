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

		public Computation() {
		}
		/**
	 * Called by the TaskMaster to start or continue computation.
	 */
		internal void Compute() {
			if (result == null && Run()) {
				if (result == null) {
					throw new InvalidOperationException("The computation " + GetType() +
					                                    " did not return a value. This is a bug.");
				}
				WakeupListeners();
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
			if (original == null) {
				return (task_master, reference, context, self, container) =>
					new FailureComputation(task_master, new SourceReference("used by override", filename,
						start_line, start_column, end_line, end_column, reference), "override of non-existant attribute");
			}
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

		protected void WakeupListeners() {
			if (consumer != null) {
				consumer(result);
				consumer = null;
			}
		}
	}

	public class FailureComputation : Computation {
		private TaskMaster task_master;
		private string message;
		private SourceReference source_reference;
		public FailureComputation(TaskMaster task_master, SourceReference reference, string message) {
			this.task_master = task_master;
			this.source_reference = reference;
			this.message = message;
		}

		protected override bool Run() {
			task_master.ReportOtherError(source_reference, message);
			return false;
		}
	}

	public interface UriHandler {
		string UriName { get; }
		Type ResolveUri(string uri, out LibraryFailure reason);
	}

/**
 * Scheduler for computations.
 */

	public abstract class TaskMaster : IEnumerable<Lookup> {
		private readonly Queue<Computation> computations = new Queue<Computation>();
		private readonly Dictionary<string, Computation> external_cache = new Dictionary<string, Computation>();
		private readonly List<UriHandler> handlers = new List<UriHandler>();
		/**
	 * These are computations that have not completed.
	 */
		private readonly Dictionary<Lookup, bool> inflight = new Dictionary<Lookup, bool>();
		private long next_id;

		public bool HasInflightLookups {
			get { return inflight.Count > 0; }
		}

		public IEnumerator<Lookup> GetEnumerator() {
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
					ReportExternalError(uri, LibraryFailure.BadName);
					return;
				}
				for (var it = 5; it < uri.Length; it++) {
					if (uri[it] != '/' && !char.IsLetterOrDigit(uri[it])) {
						ReportExternalError(uri, LibraryFailure.BadName);
						return;
					}
				}
			}

			foreach (var handler in handlers) {
				LibraryFailure reason;
				var t = handler.ResolveUri(uri, out reason);
				if (reason == LibraryFailure.Missing) {
					continue;
				}
				if (reason != LibraryFailure.None || t == null) {
					ReportExternalError(uri, reason);
					return;
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
			ReportExternalError(uri, LibraryFailure.Missing);
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

		public abstract void ReportExternalError(string uri, LibraryFailure reason);
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
			if (computation is Lookup) {
				var lookup = (Lookup) computation;
				inflight[lookup] = true;
				computation.Notify(x => inflight.Remove(lookup));
			}
			computations.Enqueue(computation);
		}

		private delegate void ReportError(string error_msg);

		public static bool VerifySymbol(Stringish strish) {
			return VerifySymbol(strish, error_msg => {});
		}
		public bool VerifySymbol(SourceReference source_reference, Stringish strish) {
			return VerifySymbol(strish, msg => ReportOtherError(source_reference, msg));
		}
		private static bool VerifySymbol(Stringish strish, ReportError error) {
			var str = strish.ToString();
			if (str.Length < 1) {
				error("An attribute name cannot be empty.");
				return false;
			}
			switch (Char.GetUnicodeCategory(str[0])) {
				case UnicodeCategory.LowercaseLetter:
				case UnicodeCategory.OtherLetter:
					break;
				default:
					error(String.Format("The name “{0}” is unbecoming of an attribute; it cannot start with “{1}”.",
						str, str[0]));
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
						error(String.Format("The name “{0}” is unbecoming of an attribute; it cannot contain “{1}”.",
							str, str[it]));
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
		private class Attempt {
			public int frame;
			public int name;
			public Frame result_frame;
			public Frame source_frame;
			Lookup owner;

			public Attempt(Lookup owner, int name, int frame, Frame source_frame) {
				this.owner = owner;
				this.name = name;
				this.frame = frame;
				this.source_frame = source_frame;
			}

			public void Consume(object return_value) {
				if (name == owner.names.Length - 1) {
					owner.result = return_value;
					owner.WakeupListeners();
				} else if (return_value is Frame) {
					result_frame = ((Frame) return_value);
					var next = new Attempt(owner, name + 1, frame, result_frame);
					owner.known_attempts.AddLast(next);
					if (result_frame.GetOrSubscribe(owner.names[name + 1], next.Consume)) {
						return;
					}
					owner.master.Slot(owner);
				} else {
					owner.master.ReportLookupError(owner, return_value.GetType());
				}
			}
		}
		public int FrameCount {
			get { return frames.Length; }
		}

		public Frame this[int name, int frame] {
			get {
				foreach(var current in known_attempts) {
					if (current.frame == frame && current.name > name
							|| current.frame > frame) {
						return null;
					}
					if (current.frame == frame && current.name == name) {
						return current.source_frame;
					}
				}
				return null;
			}
		}

		public Frame LastFrame {
			get { return known_attempts.Last.Value.source_frame; }
		}

		public string LastName {
			get { return names[known_attempts.Last.Value.name]; }
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
		private int frame_index = 0;

		private readonly Frame[] frames;

		private LinkedList<Attempt> known_attempts = new LinkedList<Attempt>();

		private readonly TaskMaster master;
		/**
	 * The name components in the lookup expression.
	 */
		private readonly string[] names;

		public Lookup(TaskMaster master, SourceReference source_ref, string[] names, Context context) {
			this.master = master;
			SourceReference = source_ref;
			this.names = names;

			/* Create  grid where the first entry is the frame under consideration. */
			frames = new Frame[context.Length];
			var index = 0;
			foreach (var frame in context.Fill()) {
				frames[index++] = frame;
			}
		}

		public string GetName(int index) {
			return names[index];
		}

		protected override bool Run() {
			while (frame_index < frames.Length) {
				int index = frame_index++;
				var root_attempt = new Attempt(this, 0, index, frames[index]);
				known_attempts.AddLast(root_attempt);
				if (frames[index].GetOrSubscribe(names[0], root_attempt.Consume)) {
					return false;
				}
			}
			master.ReportLookupError(this, null);
			return false;
		}
	}
}
