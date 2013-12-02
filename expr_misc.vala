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
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			var left_result = convert (engine, left, Ty.STR);
			var right_result = convert (engine, right, Ty.STR);
			engine.operands.push (new String (((String) left_result).value.concat (((String) right_result).value)));
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
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call (condition);
			var condition_result = engine.operands.pop ();
			if (condition_result is Boolean) {
				engine.call (((Boolean) condition_result).value ? truepart : falsepart);
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH ("Expected boolean value for condition.");
		}
	}
	internal class ContextualLookup : Expression {
		public Gee.List<Nameish> names {
			get;
			set;
		}
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call (engine.lookup_contextual (names));
		}
	}
	internal class IsDefined : Expression {
		public Gee.List<Nameish> names {
			get;
			set;
		}
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Boolean (engine.is_defined (names)));
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
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			engine.call (engine.lookup_direct (names));
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
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			var result = engine.operands.pop ();
			if (!(result is Tuple)) {
				throw new EvaluationError.TYPE_MISMATCH ("Can only do indirect look from the context of a tuple.");
			}
			var state = engine.state;
			state.context = ((Tuple) result).context;
			engine.state = state;
			engine.call (engine.lookup_contextual (names));
		}
	}
}