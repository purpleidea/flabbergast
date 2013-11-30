namespace Flabbergast {
	public abstract class Expression : Object {
		public abstract void evaluate(ExecutionEngine engine) throws EvaluationError;
	}
	public class SubExpression : Expression {
		public Expression expression { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(expression);
		}
	}
	public class TrueLiteral : Expression {
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.operands.push(new Boolean(true));
		}
	}
	public class FalseLiteral : Expression {
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.operands.push(new Boolean(false));
		}
	}
	public class IntegerLiteral : Expression {
		public int @value { get; set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.operands.push(new Integer(@value));
		}
	}
	public class FloatLiteral : Expression {
		public double @value { get; set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.operands.push(new Float(@value));
		}
	}
	public class StringLiteral : Expression {
		public string @value { get; set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.operands.push(new String(@value));
		}
	}
	public class NullLiteral : Expression {
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.operands.push(new Null());
		}
	}
	public class NullCoalesce : Expression {
		public Expression expression { get; private set; }
		public Expression alternate { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(expression);
			if (engine.operands.peek() is Null) {
				engine.operands.pop();
				engine.call(alternate);
			}
		}
	}
	public class IsNull : Expression {
		public Expression expression { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(expression);
			engine.operands.push(new Boolean(engine.operands.pop() is Null));
		}
	}
	public class Error : Expression {
		public Expression expression { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(expression);
			var result = engine.operands.pop();
			if (result is String) {
				throw new EvaluationError.USER_DEFINED(((String) result).value);
			} else {
				throw new EvaluationError.TYPE_MISMATCH("Expected string in error.");
			}
		}
	}
}