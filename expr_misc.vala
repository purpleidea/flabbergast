namespace Flabbergast.Expressions {
	public class StringConcatenate : Expression {
		public Expression left { get; private set; }
		public Expression right { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			Coerce.convert(engine, left, Ty.STR);
			Coerce.convert(engine, right, Ty.STR);
			var right_result = engine.operands.pop();
			var left_result = engine.operands.pop();
			engine.operands.push(new String(((String) left_result).value.concat(((String) right_result).value)));
		}
	}
	public class Conditional : Expression {
		public Expression condition { get; private set; }
		public Expression truepart { get; private set; }
		public Expression falsepart { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(condition);
			var condition_result = engine.operands.pop();
			if (condition_result is Boolean) {
				engine.call(((Boolean) condition_result).value ? truepart : falsepart);
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH("Expected boolean value for condition.");
		}
	}
	public class ContextualLookup : Expression {
		public string[] names;
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.lookup_contextual(names);
		}
	}
	public class IsDefined : Expression {
		public string[] names;
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.operands.push(new Boolean(engine.is_defined(names)));
		}
	}
	public class DirectLookup : Expression {
		public Expression expression { get; private set; }
		public string[] names;
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(expression);
			engine.lookup_direct(names);
		}
	}
}