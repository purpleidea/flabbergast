namespace Flabbergast {
	public abstract class Expression : Object, GTeonoma.SourceInfo {
		public GTeonoma.source_location source {
			get;
			set;
		}
		public abstract void evaluate (ExecutionEngine engine) throws EvaluationError;
	}
}
namespace Flabbergast.Expressions {
	public class ReturnLiteral : Expression {
		private unowned Datum datum;
		public ReturnLiteral (Datum datum) {
			this.datum = datum;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (datum);
		}
	}
	internal class SubExpression : Expression {
		public Expression expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
		}
	}
	internal class TrueLiteral : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Boolean (true));
		}
	}
	internal class FalseLiteral : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Boolean (false));
		}
	}
	internal class IntegerLiteral : Expression {
		public int @value {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Integer (@value));
		}
	}
	internal class FloatLiteral : Expression {
		public double @value {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Float (@value));
		}
	}
	internal class StringPiece : Object {
		public Expression? expression {
			get;
			set;
		}
		public GTeonoma.StringLiteral? literal {
			get;
			set;
		}
		public void render (ExecutionEngine engine, StringBuilder builder) throws EvaluationError {
			var result = (String) convert (engine, expression, Ty.STR);
			builder.append (result.@value);
			if (literal != null) {
				builder.append (literal.str);
			}
		}
	}
	internal class StringLiteral : Expression {
		public Gee.List<StringPiece>? contents {
			get;
			set;
		}
		public GTeonoma.StringLiteral literal {
			get;
			set;
		}

		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			var builder = new StringBuilder ();
			builder.append (literal.str);
			if (contents != null) {
				foreach (var chunk in contents) {
					chunk.render (engine, builder);
				}
			}
			engine.operands.push (new String (builder.str));
		}
	}
	internal class NullLiteral : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Null ());
		}
	}
	internal class NullCoalesce : Expression {
		public Expression expression {
			get;
			set;
		}
		public Expression alternate {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			if (engine.operands.peek () is Null) {
				engine.operands.pop ();
				engine.call (alternate);
			}
		}
	}
	internal class IsNull : Expression {
		public Expression expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			engine.operands.push (new Boolean (engine.operands.pop () is Null));
		}
	}
	internal class RaiseError : Expression {
		public Expression expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			var result = engine.operands.pop ();
			if (result is String) {
				throw new EvaluationError.USER_DEFINED (((String) result).value);
			} else {
				throw new EvaluationError.TYPE_MISMATCH ("Expected string in error.");
			}
		}
	}
}
