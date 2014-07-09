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
				foreach (var context in contexts) {
					var local_state = state;
					local_state.context = context;
					engine.state = local_state;
					engine.call ((!)where);
					var result = engine.operands.pop ();
					if (!(result is Data.Boolean)) {
						throw new EvaluationError.TYPE_MISMATCH ("Result from Where clause is not a boolean.");
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
	internal abstract class Selector : Object {
		public abstract void transform ();
		public abstract Gee.List<uint> generate_contexts (ExecutionEngine engine) throws EvaluationError;
	}
	internal abstract class Result : Object {
		public abstract void transform ();
		public abstract void generate_result (ExecutionEngine engine, Gee.List<uint> contexts) throws EvaluationError;
	}
	internal abstract class OrderClause : Object {
		public abstract void transform ();
		public abstract Gee.List<uint> reorder_contexts (ExecutionEngine engine, Gee.List<uint> contexts) throws EvaluationError;
	}
	internal class PassThrough : Selector {
		public Expression source {
			get;
			set;
		}
		public override void transform () {
			source = source.transform ();
		}
		public override Gee.List<uint> generate_contexts (ExecutionEngine engine) throws EvaluationError {
			engine.call (source);
			var container_tuple = engine.operands.pop ();
			if (!(container_tuple is Data.Tuple)) {
				throw new EvaluationError.NAME ("Value passed to Each is not a tuple.");
			}
			var contexts = new Gee.ArrayList<uint> ();
			var state = engine.state;
			foreach (Gee.Map.Entry<string, Expression> entry in (Data.Tuple)container_tuple) {
				if (entry.key[0].isupper ()) {
					continue;
				}
				engine.call (entry.value);
				var datum = engine.operands.pop ();
				if (!(datum is Data.Tuple)) {
					throw new EvaluationError.TYPE_MISMATCH (@"$(entry.key) is not a tuple in Each selector.");
				}
				var context = engine.environment.create ();
				foreach (Gee.Map.Entry<string, Expression> subentry in (Data.Tuple)datum) {
					if (entry.key[0].isupper ()) {
						continue;
					}
					engine.environment[context, subentry.key] = subentry.value;
				}
				engine.environment.append_containers (context, state.containers);
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
	internal class TupleGetter : NameGetter {
		private Data.Tuple tuple;
		internal TupleGetter (string name, Data.Tuple tuple) {
			this.name = name;
			this.tuple = tuple;
		}
		internal override Expression get (ExecutionEngine engine, string name)  throws EvaluationError {
			if (tuple.attributes.has_key (name)) {
				var expr = tuple[name];
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
	internal class TupleSource : Source {
		public Expression expression {
			get;
			set;
		}
		internal override NameGetter prepare_input (ExecutionEngine engine, Gee.Set<string> attributes) throws EvaluationError {
			engine.call (expression);
			var tuple = engine.operands.pop ();
			if (!(tuple is Data.Tuple)) {
				throw new EvaluationError.TYPE_MISMATCH (@"Value for $(name.name) is not a tuple.");
			}
			foreach (var entry in ((Data.Tuple)tuple).attributes.entries) {
				if (!entry.key[0].islower ()) {
					continue;
				}
				engine.call (entry.value);
				if (engine.operands.pop () is Data.Continue) {
					continue;
				}
				attributes.add (entry.key);
			}
			return new TupleGetter (name.name, (Data.Tuple)tuple);
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
	internal class MergedTuples : Selector {
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
					throw new EvaluationError.NAME (@"Duplicate name $(name) in For.");
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
					throw new EvaluationError.TYPE_MISMATCH ("Order key must be either an integer or a string.");
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

	internal class NamedTuple : Result {
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
			var tuple = new Data.Tuple (context);
			tuple.containers = new Utils.ContainerReference (engine.state.context, engine.state.containers);

			var state = engine.state;
			if (state.this_tuple != null) {
				var container_expr = new ReturnLiteral (state.this_tuple);
				tuple.attributes["Container"] = container_expr;
				engine.environment[context, "Container"] = container_expr;
				engine.environment.append_containers (context, new Utils.ContainerReference (state.context, state.containers));
			}

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
						throw new EvaluationError.NAME (@"The name $(attr_name) is not a legal attribute name.");
					}
				} else {
					throw new EvaluationError.TYPE_MISMATCH ("The attribute type must be an integer or a string.");
				}
				if (tuple.attributes.has_key (attr_name)) {
					throw new EvaluationError.NAME (@"Duplicate attribute name $(attr_name) in result of For⋯Select.");
				}

				var attr_value = engine.create_closure (result_value);
				tuple.attributes[attr_name]  = attr_value;
				engine.environment[context, attr_name] = attr_value;
			}
			engine.operands.push (tuple);
		}
		public override void transform () {
			result_attr = result_attr.transform ();
			result_value = result_value.transform ();
		}
	}
	internal class AnonymousTuple : Result {
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
			var tuple = new Data.Tuple (context);
			tuple.containers = new Utils.ContainerReference (engine.state.context, engine.state.containers);

			var state = engine.state;
			if (state.this_tuple != null) {
				var container_expr = new ReturnLiteral (state.this_tuple);
				tuple.attributes["Container"] = container_expr;
				engine.environment[context, "Container"] = container_expr;
				engine.environment.append_containers (context, new Utils.ContainerReference (state.context, state.containers));
			}

			for (var it = 0; it < input_contexts.size; it++) {
				state.context = input_contexts[it];
				engine.state = state;
				var attr_value = engine.create_closure (result);
				var attr_name = make_id (it);
				tuple.attributes[attr_name] = attr_value;
				engine.environment[context, attr_name] = attr_value;
			}
			engine.operands.push (tuple);
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
