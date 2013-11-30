namespace Flabbergast {
	public errordomain EvaluationError {
		CIRCULAR,
		DUPLICATE_NAME,
		EXTERNAL_REMAINING,
		INTERNAL,
		OVERRIDE,
		RESOLUTION,
		TYPE_MISMATCH,
		USER_DEFINED
	}

	internal class Promise : Expression {
		WeakRef owner;
		Expression expression;
		bool is_running = false;
		MachineState state;
		internal Datum? evaluated_form = null;

		internal Promise(ExecutionEngine engine, Expression expression, MachineState state) {
			owner = WeakRef(engine);
			this.expression = expression;
			this.state = state;
		}

		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			if (owner.get() != engine) {
				throw new EvaluationError.INTERNAL("Tried to execute a promise on a different evaluation enginge.");
			}
			if (is_running) {
				throw new EvaluationError.CIRCULAR("Circular evaluation detected.");
			}
			if (evaluated_form != null) {
				engine.operands.push(evaluated_form);
				return;
			}
			engine.state = state;
			is_running = true;
			expression.evaluate(engine);
			is_running = false;
			evaluated_form = engine.operands.peek();
		}
	}

	public class ContainerReference {
		public ContainerReference? parent;
		public uint context;
		public ContainerReference(uint context, ContainerReference? parent) {
			this.parent = parent;
			this.context = context;
		}
	}

	internal struct MachineState {
		internal uint context;
		internal ContainerReference? containers;
		internal unowned Tuple? this_tuple;
	}

	public class DataStack {
		Gee.Deque<Datum> stack = new Gee.ArrayQueue<Datum> ();
		public DataStack() {}

		public Datum? peek() {
			return (stack.size == 0) ? null : stack.peek_head();
		}
		public Datum? pop() {
			return (stack.size == 0) ? null : stack.poll_head();
		}
		public void push(Datum datum) {
			stack.offer_head(datum);
		}
	}
	public class NameEnvironment {
		Utils.DefaultMap<string, Gee.HashMap<uint, Expression> > defined_names = new Utils.DefaultMap<string, Gee.HashMap<uint, Expression> > ((key) => new Gee.HashMap<uint, Expression> ());

		Utils.DefaultMap<string, Utils.DefaultMap<uint, Gee.List<Expression> > > known_names = new Utils.DefaultMap<string, Utils.DefaultMap<uint, Gee.List<Expression> > > ((key) => new Utils.DefaultMap<uint, Gee.List<Expression> > ((key) => new Gee.ArrayList<Expression> ()));

		uint next_context = 0;

		public NameEnvironment() {}

		public uint create() {
			return ++next_context;
		}
		public void append(uint target_context, uint source_context) {
			foreach (var entry in defined_names.entries) {
				var list = known_names[entry.key][target_context];
				list.add(entry.value[source_context]);
			}
		}
		public void append_containers(uint target_context, ContainerReference inherited_contexts) {
			for (; inherited_contexts != null; inherited_contexts = inherited_contexts.parent) {
				append(target_context, inherited_contexts.context);
			}
		}

		public Expression? get(uint context, string name) {
			if (!defined_names.has_key(name)) {
				return null;
			}
			var map = defined_names[name];
			if (!map.has_key(context)) {
				return null;
			}
			return map[context];
		}

		public Gee.List<Expression> lookup(uint context, string name) {
			if (known_names.has_key(name)) {
				var map = known_names[name];
				if (map.has_key(context)) {
					return map[context];
				}
			}
			return known_names[name][context];
		}

		public void set(uint context, string name, Expression @value) {
			defined_names[name][context] = @value;
		}
	}

	public class ExecutionEngine : Object {
		StackFrame[] call_stack = {};
		public NameEnvironment environment { get; private set; default = new NameEnvironment(); }
		public DataStack operands { get; private set; default = new DataStack(); }
		internal MachineState state { get { return call_stack[call_stack.length - 1].state; } set { call_stack[call_stack.length - 1].state = value; } }

		struct StackFrame {
			internal Expression expression;
			internal MachineState state;
			internal StackFrame(MachineState state, Expression expression) {
				this.expression = expression;
				this.state = state;
			}
		}

		public void call(Expression expression) throws EvaluationError {
			call_stack += StackFrame(call_stack[call_stack.length - 1].state, expression);
			expression.evaluate(this);
			call_stack.length--;
		}

		public Expression create_closure(Expression expression) {
			return new Promise(this, expression, state);
		}

		public bool is_defined(string[] names) throws EvaluationError requires(names.length > 0) {
			var result = lookup_contextual_internal(names);
			return result != null;
		}

		private Expression? lookup_contextal_helper(Expression start_context, string[] names) throws EvaluationError {
			if (names.length == 1) {
				return start_context;
			}
			call(start_context);
			return lookup_direct_internal(names, 1);
		}

		private Expression? lookup_contextual_internal(string[] names) throws EvaluationError {
			var child = environment.get(state.context, names[0]);
			if (child != null) {
				var result = lookup_contextal_helper(child, names);
				if (result != null) {
					return result;
				}
			}
			foreach (var start_context in environment.lookup(state.context, names[0])) {
				var result = lookup_contextal_helper(start_context, names);
				if (result != null) {
					return result;
				}
			}
			return null;
		}

		public Expression lookup_contextual(string[] names) throws EvaluationError requires(names.length > 0) {
			var result = lookup_contextual_internal(names);
			if (result == null) {
				throw new EvaluationError.RESOLUTION(@"Could not resolve $(string.join(".", names)).");
			}
			return (!)result;
		}
		public Expression? lookup_direct_internal(string[] names, int start_index = 0) throws EvaluationError {
			var start = operands.pop();
			for (var it = start_index; it < names.length - 1; it++) {
				if (start is Tuple) {
					var promise = ((Tuple) start)[names[it]];
					call(promise);
					start = operands.pop();
				} else {
					return null;
				}
			}
			if (start is Tuple) {
				return ((Tuple) start)[names[names.length - 1]];
			} else {
				return null;
			}
		}

		public Expression lookup_direct(string[] names) throws EvaluationError requires(names.length > 0) {
			var result = lookup_direct_internal(names);
			if (result == null) {
				throw new EvaluationError.TYPE_MISMATCH("Tried to do a direct lookup inside a non-tuple.");
			}
			return (!)result;
		}
	}
}