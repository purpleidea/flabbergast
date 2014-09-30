namespace Flabbergast.Expressions {
	internal class LogicalAnd : Expression {
		public Expression left {
			get;
			set;
		}
		public Expression right {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (left);
			var left_value = engine.operands.peek ();
			if (left_value is Data.Boolean) {
				if (((Data.Boolean)left_value).value) {
					engine.operands.pop ();
					engine.call (right);
				}
			} else {
				throw new EvaluationError.TYPE_MISMATCH (@"Non-boolean value in logical AND. $(source.source):$(source.line):$(source.offset)");
			}
		}

		public override Expression transform () {
			left = left.transform ();
			right = right.transform ();
			return this;
		}
	}

	internal class LogicalOr : Expression {
		public Expression left {
			get;
			set;
		}
		public Expression right {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (left);
			var left_value = engine.operands.peek ();
			if (left_value is Data.Boolean) {
				if (!((Data.Boolean)left_value).value) {
					engine.operands.pop ();
					engine.call (right);
				}
			} else {
				throw new EvaluationError.TYPE_MISMATCH (@"Non-boolean value in logical OR. $(source.source):$(source.line):$(source.offset)");
			}
		}
		public override Expression transform () {
			left = left.transform ();
			right = right.transform ();
			return this;
		}
	}

	public int compare (ExecutionEngine engine, Expression expr) throws EvaluationError {
		var right = engine.operands.pop ();
		var left = engine.operands.pop ();
		if (left is Data.String && right is Data.String) {
			return ((Data.String)left).value.collate (((Data.String)right).value);
		}
		if (left is Data.Boolean && right is Data.Boolean) {
			var left_value = ((Data.Boolean)left).value;
			var right_value = ((Data.Boolean)right).value;
			return left_value == right_value ? 0 : (left_value) ? 1 : -1;
		}
		if (left is Data.Integer) {
			if (right is Data.Integer) {
				var left_value = ((Data.Integer)left).value;
				var right_value = ((Data.Integer)right).value;
				return (left_value - right_value).clamp (-1, 1);
			}
			if (right is Data.Float) {
				var left_value = (double) ((Data.Integer)left).value;
				var right_value = ((Data.Float)right).value;
				return (int) (left_value - right_value).clamp (-1, 1);
			}
		}
		if (left is Data.Float) {
			if (right is Data.Float) {
				var left_value = ((Data.Float)left).value;
				var right_value = ((Data.Float)right).value;
				return (int) (left_value - right_value).clamp (-1, 1);
			}
			if (right is Data.Integer) {
				var left_value = ((Data.Float)left).value;
				var right_value = (double) ((Data.Integer)right).value;
				return (int) (left_value - right_value).clamp (-1, 1);
			}
		}
		throw new EvaluationError.TYPE_MISMATCH (@"Incompatible types to comparison operation. $(expr.source.source):$(expr.source.line):$(expr.source.offset)");
	}

	internal class Shuttle : Expression {
		public Expression left {
			get;
			set;
		}
		public Expression right {
			get;
			set;
		}

		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (left);
			engine.call (right);
			var result = compare (engine, this);
			engine.operands.push (new Data.Integer (result));
		}
		public override Expression transform () {
			left = left.transform ();
			right = right.transform ();
			return this;
		}
	}
	internal abstract class Comparison : Expression {
		public Expression left {
			get;
			set;
		}
		public Expression right {
			get;
			set;
		}
		protected abstract bool check (int result);
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (left);
			engine.call (right);
			var result = compare (engine, this);
			engine.operands.push (new Data.Boolean (check (result)));
		}
		public override Expression transform () {
			left = left.transform ();
			right = right.transform ();
			return this;
		}
	}
	internal class Equality : Comparison {
		protected override bool check (int result) {
			return result == 0;
		}
	}
	internal class Inequality : Comparison {
		protected override bool check (int result) {
			return result != 0;
		}
	}
	internal class GreaterThan : Comparison {
		protected override bool check (int result) {
			return result > 0;
		}
	}
	internal class GreaterThanOrEqualTo : Comparison {
		protected override bool check (int result) {
			return result >= 0;
		}
	}
	internal class LessThan : Comparison {
		protected override bool check (int result) {
			return result < 0;
		}
	}
	internal class LessThanOrEqualTo : Comparison {
		protected override bool check (int result) {
			return result <= 0;
		}
	}
	internal abstract class BinaryOperation : Expression {
		public Expression left {
			get;
			set;
		}
		public Expression right {
			get;
			set;
		}
		public abstract string name {
			get;
		}
		protected abstract int compute_int (int left_value, int right_value) throws EvaluationError;
		protected abstract double compute_double (double left_value, double right_value) throws EvaluationError;
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (left);
			var left = engine.operands.pop ();
			engine.call (right);
			var right = engine.operands.pop ();
			if (left is Data.Integer && right is Data.Integer) {
				var result = compute_int (((Data.Integer)left).value, ((Data.Integer)right).value);
				engine.operands.push (new Data.Integer (result));
				return;
			}
			if ((left is Data.Integer || left is Data.Float) && (right is Data.Integer || right is Data.Float)) {
				var left_value = (left is Data.Integer) ? ((double) ((Data.Integer)left).value) : ((Data.Float)left).value;
				var right_value = (right is Data.Integer) ? ((double) ((Data.Integer)right).value) : ((Data.Float)right).value;
				engine.operands.push (new Data.Float (compute_double (left_value, right_value)));
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH (@"Invalid type to mathematical operator for $(name). $(source.source):$(source.line):$(source.offset)");
		}
		public abstract bool is_compatible (BinaryOperation op);

		public override Expression transform () {
			left = left.transform ();
			right = right.transform ();
			if (right is BinaryOperation && is_compatible ((BinaryOperation) right)) {
				var new_parent = (BinaryOperation) right;
				right = new_parent.left;
				new_parent.left = this;
				return new_parent.transform ();
			}
			return this;
		}
	}
	internal class Addition : BinaryOperation {
		public override string name {
			get {
				return "addition";
			}
		}
		protected override int compute_int (int left_value, int right_value) throws EvaluationError {
			return left_value + right_value;
		}
		protected override double compute_double (double left_value, double right_value) throws EvaluationError {
			return left_value + right_value;
		}
		public override bool is_compatible (BinaryOperation op) {
			return op is Addition || op is Subtraction;
		}
	}
	internal class Subtraction : BinaryOperation {
		public override string name {
			get {
				return "subtraction";
			}
		}
		protected override int compute_int (int left_value, int right_value) throws EvaluationError {
			return left_value - right_value;
		}
		protected override double compute_double (double left_value, double right_value) throws EvaluationError {
			return left_value - right_value;
		}
		public override bool is_compatible (BinaryOperation op) {
			return op is Addition || op is Subtraction;
		}
	}
	internal class Multiplication : BinaryOperation {
		public override string name {
			get {
				return "multiplication";
			}
		}
		protected override int compute_int (int left_value, int right_value) throws EvaluationError {
			return left_value * right_value;
		}
		protected override double compute_double (double left_value, double right_value) throws EvaluationError {
			return left_value * right_value;
		}
		public override bool is_compatible (BinaryOperation op) {
			return op is Multiplication || op is Division || op is Modulus;
		}
	}
	internal class Division : BinaryOperation {
		public override string name {
			get {
				return "division";
			}
		}
		protected override int compute_int (int left_value, int right_value) throws EvaluationError {
			if (right_value == 0) {
				throw new EvaluationError.NUMERIC (@"Division by zero. $(source.source):$(source.line):$(source.offset)");
			}
			return left_value / right_value;
		}
		protected override double compute_double (double left_value, double right_value) throws EvaluationError {
			return left_value / right_value;
		}
		public override bool is_compatible (BinaryOperation op) {
			return op is Multiplication || op is Division || op is Modulus;
		}
	}
	internal class Modulus : BinaryOperation {
		public override string name {
			get {
				return "modulus";
			}
		}
		protected override int compute_int (int left_value, int right_value) throws EvaluationError {
			if (right_value == 0) {
				throw new EvaluationError.NUMERIC (@"Modulus by zero. $(source.source):$(source.line):$(source.offset)");
			}
			return left_value % right_value;
		}
		protected override double compute_double (double left_value, double right_value) throws EvaluationError {
			throw new EvaluationError.TYPE_MISMATCH (@"Modulus is not defined for floats. $(source.source):$(source.line):$(source.offset)");
		}
		public override bool is_compatible (BinaryOperation op) {
			return op is Multiplication || op is Division || op is Modulus;
		}
	}
	internal class Not : Expression {
		public Expression expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			var result = engine.operands.pop ();
			if (result is Data.Boolean) {
				engine.operands.push (new Data.Boolean (!((Data.Boolean)result).value));
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH (@"Inavlid type for boolean not operation. $(source.source):$(source.line):$(source.offset)");
		}
		public override Expression transform () {
			expression = expression.transform ();
			return this;
		}
	}
	internal class Negation : Expression {
		public Expression expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			var result = engine.operands.pop ();
			if (result is Data.Integer) {
				engine.operands.push (new Data.Integer (-((Data.Integer)result).value));
				return;
			}
			if (result is Data.Float) {
				engine.operands.push (new Data.Float (-((Data.Float)result).value));
				return;
			}
			throw new EvaluationError.TYPE_MISMATCH (@"Inavlid type for negation operation. $(source.source):$(source.line):$(source.offset)");
		}
		public override Expression transform () {
			expression = expression.transform ();
			return this;
		}
	}
	internal class IsNaN : Expression {
		public Expression expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			var result = engine.operands.pop ();
			if (result is Data.Integer) {
				engine.operands.push (new Data.Boolean (false)); return;
			}
			if (result is Data.Float) {
				engine.operands.push (new Data.Boolean (((Data.Float)result).value.is_nan ())); return;
			}
			throw new EvaluationError.TYPE_MISMATCH (@"Invalid type to not-a-number check. $(source.source):$(source.line):$(source.offset)");
		}
		public override Expression transform () {
			expression = expression.transform ();
			return this;
		}
	}
	internal class IsFinite : Expression {
		public Expression expression {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			var result = engine.operands.pop ();
			if (result is Data.Integer) {
				engine.operands.push (new Data.Boolean (false)); return;
			}
			if (result is Data.Float) {
				engine.operands.push (new Data.Boolean (((Data.Float)result).value.is_finite ())); return;
			}
			throw new EvaluationError.TYPE_MISMATCH (@"Invalid type to infinite check. $(source.source):$(source.line):$(source.offset)");
		}
		public override Expression transform () {
			expression = expression.transform ();
			return this;
		}
	}
	internal class Through : Expression {
		public Expression start {
			get;
			set;
		}
		public Expression end {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (start);
			var start_value = engine.operands.pop ();
			engine.call (end);
			var end_value = engine.operands.pop ();
			if (!(start_value is Data.Integer)) {
				throw new EvaluationError.TYPE_MISMATCH (@"Invalid start type to Through. $(source.source):$(source.line):$(source.offset)");
			}
			if (!(end_value is Data.Integer)) {
				throw new EvaluationError.TYPE_MISMATCH (@"Invalid end type to Through. $(source.source):$(source.line):$(source.offset)");
			}
			var context = engine.environment.create ();
			var tuple = new Data.Tuple (context);
			tuple.source = source;
			tuple.containers = new Utils.ContainerReference (engine.state.context, engine.state.containers);
			var attributes = new Gee.TreeMap<string, Expression> ();
			tuple.attributes = attributes;
			var index = 0;
			var end_int = ((Data.Integer)end_value).value;
			for (var it = ((Data.Integer)start_value).value; it <= end_int; it++) {
				var attr_name = make_id (index++);
				var attr_value = new ReturnOwnedLiteral (new Data.Integer (it));
				attributes[attr_name] = attr_value;
				engine.environment[context, attr_name] = attr_value;
			}
			engine.operands.push (tuple);
			return;
		}
		public override Expression transform () {
			start = start.transform ();
			end = end.transform ();
			return this;
		}
	}
}
