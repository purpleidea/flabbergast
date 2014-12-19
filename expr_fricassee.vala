namespace Flabbergast.Expressions.Fricassee {
	internal class ForExpression : Expression {
		public Selector selector {
			get;
			set;
		}
		public Result result {
			get;
			set;
		}
		public Expression? where {
			get;
			set;
			default = null;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			var contexts = selector.generate_contexts (engine);
			if (where != null) {
				var selected_contexts = new Gee.ArrayList<uint> ();
				var state = engine.state;
				state.containers = new Utils.ContainerReference (state.context, state.containers);
				foreach (var context in contexts) {
					var local_state = state;
					local_state.context = context;
					engine.state = local_state;
					engine.call ((!)where);
					var result = engine.operands.pop ();
					if (!(result is Data.Boolean)) {
						throw new EvaluationError.TYPE_MISMATCH (@"Result from Where clause is not a boolean. $(source.source):$(source.line):$(source.offset)");
					}
					if (((Data.Boolean)result).value) {
						selected_contexts.add (context);
					}
				}
				contexts = selected_contexts;
				engine.state = state;
			}
			result.generate_result (engine, contexts);
		}
		public override Expression transform () {
			selector.transform ();
			result.transform ();
			if (where != null) {
				where = where.transform ();
			}
			return this;
		}
	}
	internal abstract class Selector : Object, GTeonoma.SourceInfo {
		public abstract void transform ();
		public abstract Gee.List<uint> generate_contexts (ExecutionEngine engine) throws EvaluationError;
		public GTeonoma.source_location source {
			get;
			set;
		}
	}
	internal abstract class Result : Object, GTeonoma.SourceInfo {
		public abstract void transform ();
		public abstract void generate_result (ExecutionEngine engine, Gee.List<uint> contexts) throws EvaluationError;
		public GTeonoma.source_location source {
			get;
			set;
		}
	}
	internal abstract class OrderClause : Object, GTeonoma.SourceInfo {
		public GTeonoma.source_location source {
			get;
			set;
		}
		public abstract void transform ();
		public abstract Gee.List<uint> reorder_contexts (ExecutionEngine engine, Gee.List<uint> contexts) throws EvaluationError;
	}
	internal class PassThrough : Selector {
		public Expression source_expr {
			get;
			set;
		}
		public override void transform () {
			source_expr = source_expr.transform ();
		}
		public override Gee.List<uint> generate_contexts (ExecutionEngine engine) throws EvaluationError {
			engine.call (source_expr);
			var container_frame = engine.operands.pop ();
			if (!(container_frame is Data.Frame)) {
				throw new EvaluationError.NAME (@"Value passed to Each is not a frame. $(source_expr.source.source):$(source_expr.source.line):$(source_expr.source.offset)");
			}
			var contexts = new Gee.ArrayList<uint> ();
			foreach (Gee.Map.Entry<string, Expression> entry in (Data.Frame)container_frame) {
				if (entry.key[0].isupper ()) {
					continue;
				}
				engine.call (entry.value);
				var datum = engine.operands.pop ();
				if (!(datum is Data.Frame)) {
					throw new EvaluationError.TYPE_MISMATCH (@"$(entry.key) is not a frame in Each selector. $(source.source):$(source.line):$(source.offset)");
				}
				var context = engine.environment.create ();
				foreach (Gee.Map.Entry<string, Expression> subentry in (Data.Frame)datum) {
					if (entry.key[0].isupper ()) {
						continue;
					}
					engine.environment[context, subentry.key] = subentry.value;
				}
				engine.environment.append (context, engine.state.context);
				engine.environment.append_containers (context, engine.state.containers);
				contexts.add (context);
			}
			return contexts;
		}
	}
	internal abstract class NameGetter {
		internal string name {
			get;
			private set;
		}
		internal abstract Expression get (ExecutionEngine engine, string name) throws EvaluationError;
	}
	internal abstract class Source : Object, GTeonoma.SourceInfo {
		public GTeonoma.source_location source {
			get;
			set;
		}
		public Name name {
			get;
			set;
		}
		internal abstract NameGetter prepare_input (ExecutionEngine engine, Gee.Set<string> attributes) throws EvaluationError;
		internal abstract void transform ();
	}
	internal class FrameGetter : NameGetter {
		private Data.Frame frame;
		internal FrameGetter (string name, Data.Frame frame) {
			this.name = name;
			this.frame = frame;
		}
		internal override Expression get (ExecutionEngine engine, string name)  throws EvaluationError {
			if (frame.attributes.has_key (name)) {
				var expr = frame[name];
				engine.call (expr);
				if (engine.operands.pop () is Data.Continue) {
					return new NullLiteral ();
				}
				return expr;
			} else {
				return new NullLiteral ();
			}
		}
	}
	internal class FrameSource : Source {
		public Expression expression {
			get;
			set;
		}
		internal override NameGetter prepare_input (ExecutionEngine engine, Gee.Set<string> attributes) throws EvaluationError {
			engine.call (expression);
			var frame = engine.operands.pop ();
			if (!(frame is Data.Frame)) {
				throw new EvaluationError.TYPE_MISMATCH (@"Value for $(name.name) is not a frame. $(source.source):$(source.line):$(source.offset)");
			}
			foreach (var entry in ((Data.Frame)frame).attributes.entries) {
				if (!entry.key[0].islower ()) {
					continue;
				}
				engine.call (entry.value);
				if (engine.operands.pop () is Data.Continue) {
					continue;
				}
				attributes.add (entry.key);
			}
			return new FrameGetter (name.name, (Data.Frame)frame);
		}
		internal override void transform () {
			expression = expression.transform ();
		}
	}
	internal class OrdinalGetter : NameGetter {
		private int index = 0;
		internal OrdinalGetter (string name) {
			this.name = name;
		}
		internal override Expression get (ExecutionEngine engine, string name)  throws EvaluationError {
			return new ReturnOwnedLiteral.int(++index);
		}
	}
	internal class OrdinalSource : Source {
		internal override NameGetter prepare_input (ExecutionEngine engine, Gee.Set<string> attributes) throws EvaluationError {
			return new OrdinalGetter (name.name);
		}
		internal override void transform () {}
	}
	internal class AttributeGetter : NameGetter {
		internal AttributeGetter (string name) {
			this.name = name;
		}
		internal override Expression get (ExecutionEngine engine, string name)  throws EvaluationError {
			return new ReturnOwnedLiteral.str (name);
		}
	}
	internal class AttributeSource : Source {
		internal override NameGetter prepare_input (ExecutionEngine engine, Gee.Set<string> attributes) throws EvaluationError {
			return new AttributeGetter (name.name);
		}
		internal override void transform () {}
	}
	internal class MergedFrames : Selector {
		public Gee.List<Source> sources {
			get;
			set;
		}
		public override void transform () {
			foreach (var source in sources) {
				source.transform ();
			}
		}
		public override Gee.List<uint> generate_contexts (ExecutionEngine engine) throws EvaluationError {
			var environment_names = new Gee.HashSet<string> ();
			foreach (var source in sources) {
				var name = source.name.name;
				if (name in environment_names) {
					throw new EvaluationError.NAME (@"Duplicate name $(name) in For. $(source.source.source):$(source.source.line):$(source.source.offset)");
				}
				environment_names.add (name);
			}

			var attr_names = new Gee.TreeSet<string> ();
			var getters = new Gee.ArrayList<NameGetter> ();
			foreach (var source in sources) {
				getters.add (source.prepare_input (engine, attr_names));
			}

			var output = new Gee.ArrayList<uint> ();
			foreach (var attr_name in attr_names) {
				var context = engine.environment.create ();
				foreach (var getter in getters) {
					engine.environment[context, getter.name] = getter[engine, attr_name];
				}
				engine.environment.append (context, engine.state.context);
				engine.environment.append_containers (context, engine.state.containers);
				output.add (context);
			}
			return output;
		}
	}
	internal class OrderBy : OrderClause {
		public Expression order {
			get;
			set;
		}
		public override void transform () {
			order = order.transform ();
		}
		public override Gee.List<uint> reorder_contexts (ExecutionEngine engine, Gee.List<uint> contexts) throws EvaluationError {
			var output = new Gee.TreeMultiMap<string, uint> ();
			var state = engine.state;
			state.containers = new Utils.ContainerReference (state.context, state.containers);
			foreach (var context in contexts) {
				var local_state = state;
				local_state.context = context;
				engine.state = local_state;
				engine.call (order);
				var order_key = engine.operands.pop ();
				if (order_key is Data.String) {
					output[((Data.String)order_key).value] = context;
				} else if (order_key is Data.Integer) {
					output[make_id (((Data.Integer)order_key).value)] = context;
				} else {
					throw new EvaluationError.TYPE_MISMATCH (@"Order key must be either an integer or a string. $(source.source):$(source.line):$(source.offset)");
				}
			}
			engine.state = state;
			var output_list = new Gee.ArrayList<uint> ();
			output_list.add_all (output.get_values ());
			return output_list;
		}
	}
	internal class Reverse : OrderClause {
		public override void transform () {}
		public override Gee.List<uint> reorder_contexts (ExecutionEngine engine, Gee.List<uint> contexts) throws EvaluationError {
			var output_list = new Gee.ArrayList<uint> ();
			for (var it = contexts.size - 1; it >= 0; it--) {
				output_list.add (contexts[it]);
			}
			return output_list;
		}
	}

	internal class NamedFrame : Result {
		public Expression result_attr {
			get;
			set;
			default = null;
		}
		public Expression result_value {
			get;
			set;
		}
		public override void generate_result (ExecutionEngine engine, Gee.List<uint> contexts) throws EvaluationError {
			var context = engine.environment.create ();
			var frame = new Data.Frame (context);
			frame.source = source;
			frame.containers = new Utils.ContainerReference (engine.state.context, engine.state.containers);

			var state = engine.state;
			state.containers = frame.containers;
			engine.environment.append_containers (context, state.containers);

			foreach (var target_context in contexts) {
				state.context = target_context;
				engine.state = state;

				string attr_name;
				engine.call (result_attr);
				var attr_name_value = engine.operands.pop ();
				if (attr_name_value is Data.Integer) {
					attr_name = make_id (((Data.Integer)attr_name_value).value);
				} else if (attr_name_value is Data.String) {
					attr_name = ((Data.String)attr_name_value).value;
					if (!Regex.match_simple ("^[a-z][a-zA-Z0-9_]", attr_name)) {
						throw new EvaluationError.NAME (@"The name $(attr_name) is not a legal attribute name. $(source.source):$(source.line):$(source.offset)");
					}
				} else {
					throw new EvaluationError.TYPE_MISMATCH (@"The attribute type must be an integer or a string. $(source.source):$(source.line):$(source.offset)");
				}
				if (frame.attributes.has_key (attr_name)) {
					throw new EvaluationError.NAME (@"Duplicate attribute name $(attr_name) in result of For⋯Select. $(source.source):$(source.line):$(source.offset)");
				}

				var attr_value = engine.create_closure (result_value);
				frame.attributes[attr_name] = attr_value;
				engine.environment[context, attr_name] = attr_value;
			}
			engine.operands.push (frame);
		}
		public override void transform () {
			result_attr = result_attr.transform ();
			result_value = result_value.transform ();
		}
	}
	internal class AnonymousFrame : Result {
		public Expression result {
			get;
			set;
		}
		public OrderClause? order {
			get;
			set;
			default = null;
		}
		public override void generate_result (ExecutionEngine engine, Gee.List<uint> contexts) throws EvaluationError {
			var input_contexts = contexts;
			if (order != null) {
				var new_contexts = order.reorder_contexts (engine, contexts);
				input_contexts = new_contexts;
			}

			var context = engine.environment.create ();
			var frame = new Data.Frame (context);
			frame.source = source;
			frame.containers = new Utils.ContainerReference (engine.state.context, engine.state.containers);

			var state = engine.state;
			engine.environment.append_containers (context, new Utils.ContainerReference (state.context, state.containers));
			state.containers = new Utils.ContainerReference (state.context, state.containers);

			for (var it = 0; it < input_contexts.size; it++) {
				state.context = input_contexts[it];
				engine.state = state;
				var attr_value = engine.create_closure (result);
				var attr_name = make_id (it);
				frame.attributes[attr_name] = attr_value;
				engine.environment[context, attr_name] = attr_value;
			}
			engine.operands.push (frame);
		}
		public override void transform () {
			result = result.transform ();
			if (order != null) {
				order.transform ();
			}
		}
	}
	internal class Reduce : Result {
		public Name initial_attr {
			get;
			set;
		}
		public Expression initial {
			get;
			set;
		}
		public Expression result {
			get;
			set;
		}
		public OrderClause? order {
			get;
			set;
			default = null;
		}
		public override void generate_result (ExecutionEngine engine, Gee.List<uint> contexts) throws EvaluationError {
			var input_contexts = contexts;
			if (order != null) {
				var new_contexts = order.reorder_contexts (engine, contexts);
				input_contexts = new_contexts;
			}
			engine.call (initial);
			var state = engine.state;
			state.containers = new Utils.ContainerReference (state.context, state.containers);
			foreach (var context in input_contexts) {
				var initial_value = engine.operands.pop ();
				engine.environment[context, initial_attr.name] = new ReturnOwnedLiteral (initial_value);
				state.context = context;
				engine.state = state;
				engine.call (result);
			}
		}
		public override void transform () {
			initial = initial.transform ();
			result = result.transform ();
			if (order != null) {
				order.transform ();
			}
		}
	}
}
