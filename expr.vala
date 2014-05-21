namespace Flabbergast {
	public abstract class Expression : Object, GTeonoma.SourceInfo {
		public GTeonoma.source_location source {
			get;
			set;
		}
		public abstract void evaluate (ExecutionEngine engine) throws EvaluationError;
		public abstract Expression transform ();
	}
}
namespace Flabbergast.Expressions {
	public class ReturnLiteral : Expression {
		private unowned Data.Datum datum;
		public ReturnLiteral (Data.Datum datum) {
			this.datum = datum;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (datum);
		}
		public override Expression transform () {
			return this;
		}
	}
	public class ReturnOwnedLiteral : Expression {
		private Data.Datum datum;
		public ReturnOwnedLiteral (Data.Datum datum) {
			this.datum = datum;
		}
		public ReturnOwnedLiteral.int (int @value) {
			this(new Data.Integer (@value));
		}
		public ReturnOwnedLiteral.str (string @value) {
			this(new Data.String (@value));
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (datum);
		}
		public override Expression transform () {
			return this;
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
		public override Expression transform () {
			expression = expression.transform (); return this;
		}
	}
	internal class TrueLiteral : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Boolean (true));
		}
		public override Expression transform () {
			return this;
		}
	}
	internal class FalseLiteral : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Boolean (false));
		}
		public override Expression transform () {
			return this;
		}
	}
	internal class IntegerLiteral : Expression {
		public int @value {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Integer (@value));
		}
		public override Expression transform () {
			return this;
		}
	}
	internal class IntMaxLiteral : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Integer (int.MAX));
		}
		public override Expression transform () {
			return this;
		}
	}
	internal class IntMinLiteral : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Integer (int.MIN));
		}
		public override Expression transform () {
			return this;
		}
	}
	internal class FloatLiteral : Expression {
		public double @value {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Float (@value));
		}
		public override Expression transform () {
			return this;
		}
	}
	internal abstract class FixedFloatLiteral : Expression {
		public abstract double get_value ();
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Float (get_value ()));
		}
		public override Expression transform () {
			return this;
		}
	}
	internal class FloatMaxLiteral : FixedFloatLiteral {
		public override double get_value () {
			return double.MAX;
		}
	}
	internal class FloatMinLiteral : FixedFloatLiteral {
		public override double get_value () {
			return double.MIN;
		}
	}
	internal class FloatInfinityLiteral : FixedFloatLiteral {
		public override double get_value () {
			return double.INFINITY;
		}
	}
	internal class FloatNaNLiteral : FixedFloatLiteral {
		public override double get_value () {
			return double.NAN;
		}
	}
	internal class IdentifierStringLiteral : Expression {
		public Name name {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.String (name.name));
		}
		public override Expression transform () {
			return this;
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
			var result = (Data.String)convert (engine, expression, Data.Ty.STR);
			builder.append (result.@value);
			if (literal != null) {
				builder.append (literal.str);
			}
		}
		public void transform () {
			if (expression != null) {
				expression = expression.transform ();
			}
		}
	}
	internal class StringLiteral : Expression {
		public Gee.List<StringPiece>? contents {
			get;
			set;
		}
		public GTeonoma.StringLiteral? literal {
			get;
			set;
		}

		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			var builder = new StringBuilder ();
			if (literal != null) {
				builder.append (literal.str);
			}
			if (contents != null) {
				foreach (var chunk in contents) {
					chunk.render (engine, builder);
				}
			}
			engine.operands.push (new Data.String (builder.str));
		}
		public override Expression transform () {
			if (contents != null) {
				foreach (var piece in contents) {
					piece.transform ();
				}
			}
			return this;
		}
	}
	internal class NullLiteral : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Null ());
		}
		public override Expression transform () {
			return this;
		}
	}
	internal class ContinueLiteral : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (new Data.Continue ());
		}
		public override Expression transform () {
			return this;
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
			if (engine.operands.peek () is Data.Null) {
				engine.operands.pop ();
				engine.call (alternate);
			}
		}
		public override Expression transform () {
			expression = expression.transform (); alternate = alternate.transform (); return this;
		}
	}
	internal class IsNull : Expression {
		public Expression expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			engine.operands.push (new Data.Boolean (engine.operands.pop () is Data.Null));
		}
		public override Expression transform () {
			expression = expression.transform (); return this;
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
			if (result is Data.String) {
				throw new EvaluationError.USER_DEFINED (((Data.String)result).value);
			} else {
				throw new EvaluationError.TYPE_MISMATCH ("Expected string in error.");
			}
		}
		public override Expression transform () {
			expression = expression.transform (); return this;
		}
	}
}
