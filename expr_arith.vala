namespace Flabbergast.Expressions {
	public class LogicalAnd : Expression {
		public Expression left { get; private set; }
		public Expression right { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(left);
			var left_value = engine.operands.peek();
			if (left_value is Boolean) {
				if (((Boolean) left_value).value) {
					engine.operands.pop();
					engine.call(right);
				}
			} else {
				throw new EvaluationError.TYPE_MISMATCH("Non-boolean value in logical AND.");
			}
		}
	}

	public class LogicalOr : Expression {
		public Expression left { get; private set; }
		public Expression right { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(left);
			var left_value = engine.operands.peek();
			if (left_value is Boolean) {
				if (!((Boolean) left_value).value) {
					engine.operands.pop();
					engine.call(right);
				}
			} else {
				throw new EvaluationError.TYPE_MISMATCH("Non-boolean value in logical OR.");
			}
		}
	}

	public class Shuttle : Expression {
		public Expression left { get; private set; }
		public Expression right { get; private set; }

		public static int compare(ExecutionEngine engine) throws EvaluationError {
			var right = engine.operands.pop();
			var left = engine.operands.pop();
			if (left is String && right is String) {
				return ((String) left).value.collate(((String) right).value);
			}
			if (left is Boolean && right is Boolean) {
				var left_value = ((Boolean) left).value;
				var right_value = ((Boolean) right).value;
				return left_value == right_value ? 0 : (left_value) ? 1 : -1;
			}
			if (left is Integer) {
				if (right is Integer) {
					var left_value = ((Integer) left).value;
					var right_value = ((Integer) right).value;
					return (left_value - right_value).clamp(-1, 1);
				}
				if (right is Float) {
					var left_value = (double) ((Integer) left).value;
					var right_value = ((Float) right).value;
					return (int) (left_value - right_value).clamp(-1, 1);
				}
			}
			if (left is Float) {
				if (right is Float) {
					var left_value = ((Float) left).value;
					var right_value = ((Float) right).value;
					return (int) (left_value - right_value).clamp(-1, 1);
				}
				if (right is Integer) {
					var left_value = ((Float) left).value;
					var right_value = (double) ((Integer) right).value;
					return (int) (left_value - right_value).clamp(-1, 1);
				}
			}
			throw new EvaluationError.TYPE_MISMATCH("Incompatible types to comparison operation.");
		}

		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(left);
			engine.call(right);
			var result = compare(engine);
			engine.operands.push(new Integer(result));
		}
	}
	public abstract class Comparison : Expression {
		public Expression left { get; private set; }
		public Expression right { get; private set; }
		protected abstract bool check(int result);
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(left);
			engine.call(right);
			var result = Shuttle.compare(engine);
			engine.operands.push(new Boolean(check(result)));
		}
	}
	public class Equality : Comparison {
		protected override bool check(int result) {
			return result == 0;
		}
	}
	public class Inequality : Comparison {
		protected override bool check(int result) {
			return result != 0;
		}
	}
	public class GreaterThan : Comparison {
		protected override bool check(int result) {
			return result > 0;
		}
	}
	public class GreaterThanOrEqualTo : Comparison {
		protected override bool check(int result) {
			return result >= 0;
		}
	}
	public class LessThan : Comparison {
		protected override bool check(int result) {
			return result < 0;
		}
	}
	public class LessThanOrEqualTo : Comparison {
		protected override bool check(int result) {
			return result <= 0;
		}
	}
	public abstract class BinaryOperation : Expression {
		public Expression left { get; private set; }
		public Expression right { get; private set; }
		public abstract string name { get; }
		protected abstract int compute_int(int left_value, int right_value);
		protected abstract double compute_double(double left_value, double right_value);
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(left);
			var left = engine.operands.pop();
			engine.call(right);
			var right = engine.operands.pop();
			if (left is Integer && right is Integer) {
				var result = compute_int(((Integer) left).value, ((Integer) right).value);
				engine.operands.push(new Integer(result));
			}
			if ((left is Integer || left is Float) && (right is Integer || right is Float)) {
				var left_value = (left is Integer) ? ((double) ((Integer) left).value) : ((Float) left).value;
				var right_value = (right is Integer) ? ((double) ((Integer) right).value) : ((Float) right).value;
				engine.operands.push(new Float(compute_double(left_value, right_value)));
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH(@"Invalid type to mathematical operator for $(name).");
		}
	}
	public class Addition : BinaryOperation {
		public override string name { get { return "addition"; } }
		protected override int compute_int(int left_value, int right_value) {
			return left_value + right_value;
		}
		protected override double compute_double(double left_value, double right_value) {
			return left_value + right_value;
		}
	}
	public class Subtraction : BinaryOperation {
		public override string name { get { return "subtraction"; } }
		protected override int compute_int(int left_value, int right_value) {
			return left_value - right_value;
		}
		protected override double compute_double(double left_value, double right_value) {
			return left_value - right_value;
		}
	}
	public class Multiplication : BinaryOperation {
		public override string name { get { return "multiplication"; } }
		protected override int compute_int(int left_value, int right_value) {
			return left_value * right_value;
		}
		protected override double compute_double(double left_value, double right_value) {
			return left_value * right_value;
		}
	}
	public class Division : BinaryOperation {
		public override string name { get { return "division"; } }
		protected override int compute_int(int left_value, int right_value) {
			return left_value / right_value;
		}
		protected override double compute_double(double left_value, double right_value) {
			return left_value / right_value;
		}
	}
	public class Modulus : Expression {
		public Expression left { get; private set; }
		public Expression right { get; private set; }
		public override override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(left);
			var left = engine.operands.pop();
			engine.call(right);
			var right = engine.operands.pop();
			if (left is Integer && right is Integer) {
				engine.operands.push(new Integer(((Integer) left).value % ((Integer) right).value));
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH("Invalid type to mathematical operator for modulus.");
		}
	}
	public class Not : Expression {
		public Expression expression { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(expression);
			var result = engine.operands.pop();
			if (result is Boolean) {
				engine.operands.push(new Boolean(((Boolean) result).value));
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH("Inavlid type for boolean not operation.");
		}
	}
	public class Negation : Expression {
		public Expression expression { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(expression);
			var result = engine.operands.pop();
			if (result is Integer) {
				engine.operands.push(new Integer(-((Integer) result).value));
				return;
			}
			if (result is Float) {
				engine.operands.push(new Float(-((Float) result).value));
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH("Inavlid type for negation operation.");
		}
	}
	public class IsNaN : Expression {
		public Expression expression { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(expression);
			var result = engine.operands.pop();
			if (result is Integer) {
				engine.operands.push(new Boolean(false)); return;
			}
			if (result is Float) {
				engine.operands.push(new Boolean(((Float) result).value.is_nan())); return;
			}
			throw new EvaluationError.TYPE_MISMATCH("Invalid type to not-a-number check.");
		}
	}
	public class IsFinte : Expression {
		public Expression expression { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(expression);
			var result = engine.operands.pop();
			if (result is Integer) {
				engine.operands.push(new Boolean(false)); return;
			}
			if (result is Float) {
				engine.operands.push(new Boolean(((Float) result).value.is_finite())); return;
			}
			throw new EvaluationError.TYPE_MISMATCH("Invalid type to infinite check.");
		}
	}
}