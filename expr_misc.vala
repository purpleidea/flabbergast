namespace Flabbergast.Expressions {
	internal class StringConcatenate : Expression {
		public Expression left {
			get;
			set;
		}
		public Expression right {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			var left_result = convert (engine, left, Data.Ty.STR);
			var right_result = convert (engine, right, Data.Ty.STR);
			engine.operands.push (new Data.String (((Data.String)left_result).value.concat (((Data.String)right_result).value)));
		}
		public override Expression transform () {
			left = left.transform (); right = right.transform (); return this;
		}
	}
	internal class Conditional : Expression {
		public Expression condition {
			get;
			set;
		}
		public Expression truepart {
			get;
			set;
		}
		public Expression falsepart {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (condition);
			var condition_result = engine.operands.pop ();
			if (condition_result is Data.Boolean) {
				engine.call (((Data.Boolean)condition_result).value ? truepart : falsepart);
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH ("Expected boolean value for condition.");
		}
		public override Expression transform () {
			condition = condition.transform (); truepart = truepart.transform (); falsepart = falsepart.transform (); return this;
		}
	}
	internal class ContextualLookup : Expression {
		public Gee.List<Nameish> names {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (engine.lookup_contextual (names));
		}
		public override Expression transform () {
			return this;
		}
	}
	internal class IsDefined : Expression {
		public Gee.List<Nameish> names {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Boolean (engine.is_defined (names)));
		}
		public override Expression transform () {
			return this;
		}
	}
	internal class DirectLookup : Expression {
		public Expression expression {
			get;
			set;
		}
		public Gee.List<Nameish> names {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			engine.call (engine.lookup_direct (names));
		}
		public override Expression transform () {
			expression = expression.transform (); return this;
		}
	}
	internal class IndirectLookup : Expression {
		public Expression expression {
			get;
			set;
		}
		public Gee.List<Nameish> names {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			var result = engine.operands.pop ();
			if (!(result is Data.Tuple)) {
				throw new EvaluationError.TYPE_MISMATCH ("Can only do indirect look from the context of a tuple.");
			}
			var state = engine.state;
			state.context = ((Data.Tuple)result).context;
			engine.state = state;
			engine.call (engine.lookup_contextual (names));
		}
		public override Expression transform () {
			expression = expression.transform (); return this;
		}
	}
	internal class Let : Expression {
		public Gee.List<Attribute> attributes {
			get; set;
		}
		public Expression expression {
			get; set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			var context = engine.environment.create ();
			var tuple = new Data.Tuple (context);

			var attr_names = new Gee.HashSet<string> ();
			foreach (var attr in attributes) {
				if (attr.name.name in attr_names) {
					throw new EvaluationError.NAME (@"Duplicate attribute name $(attr.name.name).");
				}
				var attr_value = engine.create_closure (attr.expression);
				tuple.attributes[attr.name.name] = attr_value;
				engine.environment[context, attr.name.name] = attr_value;
				attr_names.add (attr.name.name);
			}

			var state = engine.state;
			if (state.this_tuple != null) {
				var container_expr = new ReturnLiteral (tuple);
				tuple.attributes["Container"] = container_expr;
				engine.environment[context, "Container"] = container_expr;
			}
			state.containers = new Utils.ContainerReference (state.context, state.containers);
			engine.environment.append_containers (context, state.containers);
			state.context = context;
			state.this_tuple = tuple;

			engine.state = state;
			engine.call (expression);
		}
		public override Expression transform () {
			foreach (var attribute in attributes) {
				attribute.expression = attribute.expression.transform ();
			}
			expression = expression.transform ();
			return this;
		}
	}
}
