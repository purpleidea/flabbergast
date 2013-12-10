namespace Flabbergast.Expressions {
	internal abstract class Fricassee : Expression {
		public Name? attr_name {
			get;
			set;
			default = null;
		}
		public bool ordinal {
			get;
			set;
			default = false;
		}
		public Gee.List<Name> names {
			get;
			set;
		}
		public Gee.List<Expression> inputs {
			get;
			set;
		}
		public Expression? where {
			get;
			set;
		}
		public Expression? order_by {
			get;
			set;
		}
		protected void evaluation_helper (ExecutionEngine engine, out Gee.MultiMap<string, uint> output) throws EvaluationError {
			if (names.size != inputs.size || names.size < 1) {
				throw new EvaluationError.BAD_MATCH (@"There number of names ($(names.size)) does not match the number of expression to assign ($(inputs.size)) in For.");
			}
			var input_map = new Gee.HashMap<string, Data.Tuple> ();
			for (var it = 0; it < names.size; it++) {
				if (input_map.has_key (names[it].name)) {
					throw new EvaluationError.NAME (@"Duplicate name $(names[it].name) in For.");
				}
				engine.call (inputs[it]);
				var result = engine.operands.pop ();
				if (!(result is Data.Tuple)) {
					throw new EvaluationError.NAME (@"$(names[it].name) is not a tuple.");
				}

				input_map[names[it].name] = (Data.Tuple)result;
			}

			var original_context = engine.state.context;

			var keys = new Gee.TreeSet<string> ();
			foreach (var input in input_map.values) {
				keys.add_all (input.attributes.keys);
			}

			output = new Gee.TreeMultiMap<string, uint> ();
			var index = 0;
			foreach (var key in keys) {
				if (!key[0].islower ()) {
					continue;
				}
				var context = engine.environment.create ();
				if (attr_name != null) {
					Data.Datum attr_key;
					if (ordinal) {
						attr_key = new Data.Integer (index++);
					} else {
						attr_key =  new Data.String (key);
					}
					engine.environment[context, attr_name.name] = new ReturnOwnedLiteral (attr_key);
				}
				foreach (var entry in input_map.entries) {
					engine.environment[context, entry.key] =
						entry.value.attributes.has_key (key)?
						entry.value.attributes[key] : new NullLiteral ();
				}
				engine.environment.append (context, original_context);
				var state = engine.state;
				state.context = context;
				engine.state = state;

				if (where != null) {
					engine.call (where);
					var result = engine.operands.pop ();
					if (!(result is Data.Boolean)) {
						throw new EvaluationError.TYPE_MISMATCH ("Result from Where clause is not a boolean.");
					}
					if (!((Data.Boolean)result).value) {
						continue;
					}
				}
				if (order_by != null) {
					engine.call (order_by);
					var order_key = engine.operands.pop ();
					if (order_key is Data.String) {
						output[((Data.String)order_key).value] = context;
					} else if (order_key is Data.Integer) {
						output[make_id (((Data.Integer)order_key).value)] = context;
					} else {
						throw new EvaluationError.TYPE_MISMATCH ("Order key must be either an integer or a string.");
					}
				} else {
					output[key] = context;
				}
			}
		}
		public override Expression transform () {
			for (var it = 0; it < inputs.size; it++) {
				inputs[it] = inputs[it].transform ();
			}
			if (where != null) {
				where = where.transform ();
			}
			if (order_by != null) {
				order_by = order_by.transform ();
			}
			return this;
		}
	}
	internal class Select : Fricassee {
		public Expression? result_attr {
			get;
			set;
			default = null;
		}
		public Expression result_expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			if (result_attr != null && order_by != null) {
				throw new EvaluationError.INTERNAL ("Cannot have a result attribute name and an Order By clause in For⋯Select.");
			}
			Gee.MultiMap<string, uint> output_contexts;
			evaluation_helper (engine, out output_contexts);

			var context = engine.environment.create ();
			var tuple = new Data.Tuple (context);

			var state = engine.state;
			if (state.this_tuple != null) {
				var container_expr = new ReturnLiteral (state.this_tuple);
				tuple.attributes["Container"] = container_expr;
				engine.environment[context, "Container"] = container_expr;
			}

			var g_type = Type.INVALID;
			var index = 0;
			foreach (var key in output_contexts.get_keys ()) {
				foreach (var @value in output_contexts[key]) {
					var local_state = engine.state;
					local_state.context = @value;
					engine.state = local_state;
					string attr_name;
					if (result_attr != null) {
						engine.call (result_attr);
						var attr_name_value = engine.operands.pop ();
						if (index == 0) {
							g_type = attr_name_value.g_type;
						} else if (attr_name_value.g_type != g_type) {
							throw new EvaluationError.TYPE_MISMATCH ("The attribute names must be of different types.");
						}
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
					} else {
						attr_name = make_id (index);
					}
					var attr_value = engine.create_closure (result_expression);
					tuple.attributes[attr_name]  = attr_value;
					engine.environment[context, attr_name] = attr_value;
					index++;
				}
			}
			engine.operands.push (tuple);
		}
		public override Expression transform () {
			if (result_attr != null) {
				result_attr = result_attr.transform ();
			}
			result_expression = result_expression.transform ();
			return base.transform ();
		}
	}
	internal class Reduce : Fricassee {
		public Name initial_attr {
			get;
			set;
			default = null;
		}
		public Expression initial_expression {
			get;
			set;
		}
		public Expression result_expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			Gee.MultiMap<string, uint> output_contexts;
			evaluation_helper (engine, out output_contexts);

			var initial_value = engine.create_closure (initial_expression);

			foreach (var key in output_contexts.get_keys ()) {
				foreach (var @value in output_contexts[key]) {
					var local_state = engine.state;
					local_state.context = @value;
					engine.state = local_state;
					engine.environment[@value, initial_attr.name] = initial_value;

					initial_value = engine.create_closure (result_expression);
				}
			}
			engine.call (initial_value);
		}
		public override Expression transform () {
			initial_expression = initial_expression.transform ();
			result_expression = result_expression.transform ();
			return base.transform ();
		}
	}
	internal class Descendent : Expression {
		public Name name {
			get;
			set;
		}
		public Expression input {
			get;
			set;
		}
		public Expression? where {
			get;
			set;
			default = null;
		}
		public Expression? stop {
			get;
			set;
			default = null;
		}
		private delegate void Match (Data.Tuple tuple);

		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (input);
			var result = engine.operands.pop ();
			if (!(result is Data.Tuple)) {
				throw new EvaluationError.TYPE_MISMATCH ("Input expression must be a tuple.");
			}

			var evaluation_context = engine.environment.create ();

			var context = engine.environment.create ();
			var tuple = new Data.Tuple (context);

			var state = engine.state;
			if (state.this_tuple != null) {
				var container_expr = new ReturnLiteral (state.this_tuple);
				tuple.attributes["Container"] = container_expr;
				engine.environment[context, "Container"] = container_expr;
			}

			var index = 0;
			state.context = evaluation_context;
			engine.state = state;
			investigate (engine, (Data.Tuple)result, evaluation_context, (match) => {
					     var index_name = make_id (index++);
					     var match_expr = new ReturnOwnedLiteral (match);
					     tuple.attributes[index_name] = match_expr;
					     engine.environment[context, index_name] = match_expr;
				     });

			engine.operands.push (tuple);
		}
		private void investigate (ExecutionEngine engine, Data.Tuple source, uint context, Match match) throws EvaluationError {
			foreach (var entry in source) {
				if (!entry.key[0].islower ()) {
					continue;
				}
				engine.call (entry.value);
				var result = engine.operands.pop ();
				if (result is Data.Tuple) {
					engine.environment[context, name.name] = new ReturnLiteral (result);

					if (where != null) {
						engine.call (where);
						var where_value = engine.operands.pop ();
						if (where_value is Data.Boolean) {
							if (((Data.Boolean)where_value).value) {
								match ((Data.Tuple)result);
							} else {}
						} else {
							throw new EvaluationError.TYPE_MISMATCH ("Where clause result must be boolean.");
						}
					} else {
						match ((Data.Tuple)result);
					}

					if (stop != null) {
						engine.call (stop);
						var stop_value = engine.operands.pop ();
						if (stop_value is Data.Boolean) {
							if (!((Data.Boolean)stop_value).value) {
								investigate (engine, (Data.Tuple)result, context, match);
							}
						} else {
							throw new EvaluationError.TYPE_MISMATCH ("Stop clause result must be boolean.");
						}
					} else {
						investigate (engine, (Data.Tuple)result, context, match);
					}
				}
			}
		}
		public override Expression transform () {
			if (where != null) {
				where = where.transform ();
			}
			if (stop != null) {
				stop = stop.transform ();
			}
			input = input.transform ();
			return this;
		}
	}
}
