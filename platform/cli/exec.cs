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

		protected readonly TaskMaster task_master;

		private Object ex = new Object();
		private bool virgin = true;

		public Computation(TaskMaster task_master) {
			this.task_master = task_master;
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
			Notify(new_consumer, true);
		}

		public void NotifyDelayed(ConsumeResult new_consumer) {
			Notify(new_consumer, false);
		}

		public void Notify(ConsumeResult new_consumer, bool needs_slot) {
			Monitor.Enter(ex);
			if (result == null) {
				if (consumer == null) {
					consumer = new_consumer;
				} else {
					consumer += new_consumer;
				}
				if (needs_slot) {
					SlotHelper();
				}
				Monitor.Exit(ex);
			} else {
				Monitor.Exit(ex);
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
					new FailureComputation(task_master, new BasicSourceReference("used by override", filename,
						start_line, start_column, end_line, end_column, reference), "override of non-existant attribute");
			}
			return
				(task_master, reference, context, self, container) =>
					wrapper(task_master, reference, context, self, container,
						original(task_master,
							new BasicSourceReference("used by override", filename, start_line, start_column, end_line,
								end_column, reference), context, self, container));
		}

		/**
	 * The method that will be invoked when the result is needed. If the method
	 * returns true, the computation is finished. Otherwise, it is assumed that
	 * the computation needs to wait another value.
	 */
		protected abstract bool Run();

		public void Slot() {
			Monitor.Enter(ex);
			if (result == null) {
				SlotHelper();
			}
			Monitor.Exit(ex);
		}

		private void SlotHelper() {
			if (virgin && task_master != null) {
				virgin = false;
				task_master.Slot(this);
			}
		}

		protected void WakeupListeners() {
			if (result == null) {
				throw new InvalidOperationException();
			}
			Monitor.Enter(ex);
			var consumer_copy = consumer;
			consumer = null;
			Monitor.Exit(ex);
			if (consumer_copy != null) {
				consumer_copy(result);
			}
		}
	}

	public class FailureComputation : Computation {
		private string message;
		private SourceReference source_reference;
		public FailureComputation(TaskMaster task_master, SourceReference reference, string message) : base(task_master) {
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
		Computation ResolveUri(TaskMaster task_master, string uri, out LibraryFailure reason);
	}

	public interface UriLoader {
		string UriName { get; }
		Type ResolveUri(string uri, out LibraryFailure reason);
	}

	public class UriInstaniator : UriHandler {
		private UriLoader loader;
		public UriInstaniator(UriLoader loader) {
			this.loader = loader;
		}
		public string UriName { get { return loader.UriName; } }
		public Computation ResolveUri(TaskMaster task_master, string uri, out LibraryFailure reason) {
			var type = loader.ResolveUri(uri, out reason);
			if (reason != LibraryFailure.None || type == null) {
				return null;
			}
			if (!typeof(Computation).IsAssignableFrom(type)) {
				throw new InvalidCastException(String.Format(
					"Class {0} for URI {1} from {2} is not a computation.", type, uri, UriName));
			}
			return (Computation) Activator.CreateInstance(type, task_master);
		}
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

		public void AddUriHandler(UriLoader loader) {
			handlers.Add(new UriInstaniator(loader));
		}

		private static char[] CreateOrdinalSymbols() {
			var array = new char[26];
			for (var it = 0; it < 26; it++) {
				array[it] = (char) ('A' + it);
			}
			Array.Sort(array);
			return array;
		}

		public virtual void GetExternal(string uri, ConsumeResult target) {
			if (external_cache.ContainsKey(uri)) {
				external_cache[uri].Notify(target);
				return;
			}
			if (uri.StartsWith("lib:")) {
				if (uri.Length < 5) {
					external_cache[uri] = BlackholeComputation.INSTANCE;
					ReportExternalError(uri, LibraryFailure.BadName);
					return;
				}
				for (var it = 5; it < uri.Length; it++) {
					if (uri[it] != '/' && !char.IsLetterOrDigit(uri[it])) {
						external_cache[uri] = BlackholeComputation.INSTANCE;
						ReportExternalError(uri, LibraryFailure.BadName);
						return;
					}
				}
			}

			foreach (var handler in handlers) {
				LibraryFailure reason;
				var computation = handler.ResolveUri(this, uri, out reason);
				if (reason == LibraryFailure.Missing) {
					continue;
				}
				if (reason != LibraryFailure.None || computation == null) {
					external_cache[uri] = BlackholeComputation.INSTANCE;
					ReportExternalError(uri, reason);
					return;
				}
				external_cache[uri] = computation;
				computation.Notify(target);
				return;
			}
			external_cache[uri] = BlackholeComputation.INSTANCE;
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
				computation.NotifyDelayed(x => inflight.Remove(lookup));
			}
			computations.Enqueue(computation);
		}

		private delegate void ReportError(string error_msg);

		public static bool VerifySymbol(Stringish strish) {
			return VerifySymbol(strish.ToString());
		}
		public static bool VerifySymbol(string str) {
			return VerifySymbol(str, error_msg => {});
		}
		public bool VerifySymbol(SourceReference source_reference, Stringish strish) {
			return VerifySymbol(source_reference, strish.ToString());
		}
		public bool VerifySymbol(SourceReference source_reference, string str) {
			return VerifySymbol(str, msg => ReportOtherError(source_reference, msg));
		}
		private static bool VerifySymbol(string str, ReportError error) {
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
		public static ComputeValue Do(params string[] names) {
			return (task_master, reference, context, self, container) => {
				foreach (var name : names) {
					if (!task_master.VerifySymbol(source_ref, name)) {
						return BlackholeComputation.INSTANCE;
					}
				}
				return new Lookup(task_master, reference, names, context);
			};
		}

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
					owner.task_master.Slot(owner);
				} else {
					owner.task_master.ReportLookupError(owner, return_value.GetType());
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

		/**
	 * The name components in the lookup expression.
	 */
		private readonly string[] names;

		public Lookup(TaskMaster task_master, SourceReference source_ref, string[] names, Context context) : base(task_master) {
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
			task_master.ReportLookupError(this, null);
			return false;
		}
	}

	/**
	 * Holds a value for inclusion of a pre-computed value in a template.
	 */
	public class Precomputation : Computation {
		public static ComputeValue Capture(object result) {
			return new Precomputation(result).ComputeValue;
		}
		public Precomputation(object result) : base(null) {
			this.result = result;
		}
		public Computation ComputeValue(
			TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container) {
			return this;
		}
		protected override bool Run() {
			return true;
		}
	}

	/**
	 * A computation that never completes.
	 */
	public class BlackholeComputation : Computation {
		public readonly static Computation INSTANCE = new BlackholeComputation();
		private BlackholeComputation() : base(null) {
		}
		protected override bool Run() {
			return false;
		}
	}

	/**
	 * Adapt C#'s async/await system to Flabbergast's TaskMaster.
	 */
	public abstract class AsyncComputation : Computation {
		private class AsyncResult : IAsyncResult {
				private readonly object state;
				private readonly AsyncCallback callback;
				private readonly AsyncComputation owner;
				private ManualResetEvent reset_event = new ManualResetEvent(false);
				private int interlock = 2;

				public AsyncResult(AsyncComputation owner, AsyncCallback callback, object state) {
					this.owner = owner;
					this.callback = callback;
					this.state = state;
				}

        public object AsyncState { get { return state; } }

        public WaitHandle AsyncWaitHandle { get { return reset_event; } }

        public bool CompletedSynchronously { get; internal set; }

        public bool HasCallback { get { return this.callback != null; } }

        public bool IsCompleted { get { return Result != null; } }

				internal object Result = null;

				public void Continue() {
					callback(this);
				}

				public void Prepare(Computation c) {
					c.Notify(result => {
						this.Result = result;
						reset_event.Set();
						if (Interlocked.Decrement(ref interlock) == 0) {
							owner.task_master.Slot(owner);
						}
					});
					if (Interlocked.Decrement(ref interlock) == 0) {
						CompletedSynchronously = true;
						Continue();
					}
				}
		}

		private AsyncResult state = null;

		public AsyncComputation(TaskMaster task_master) : base(task_master) {
		}

		/**
		 * Asynchronously wait for the result of a computation.
		 *
		 * Call `await Go(computation)` to wait for the result.
		 */
		public IAsyncResult BeginGo(Computation c, AsyncCallback callback, object asyncState) {
			state = new AsyncResult(this, callback, asyncState);
			state.Prepare(c);
			return state;
		}

		public object EndGo(IAsyncResult r)
		{
			AsyncResult ar = r as AsyncResult;
			return ar.Result;
		}

		/**
		 * Override with a user computation.
		 */
		protected abstract void RunAsync();

		protected override bool Run() {
			if (state == null) {
				Action action = RunAsync;
				action.BeginInvoke((ar) => { action.EndInvoke(ar); if (result != null) WakeupListeners(); }, this);
			} else {
				var x = state;
				state = null;
				x.Continue();
			}
			return false;
		}
	}
}
