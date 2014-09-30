namespace Flabbergast.Expressions {
	internal class TypeCheck : Expression {
		public Expression expression {
			get;
			set;
		}
		public Data.Ty ty {
			get;
			set;
		}

		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			var result = engine.operands.pop ();
			engine.operands.push (new Data.Boolean (result.get_type () == ty.get_real_type ()));
		}
		public override Expression transform () {
			expression = expression.transform (); return this;
		}
	}
	internal class TypeEnsure : Expression {
		public Expression expression {
			get;
			set;
		}
		public Data.Ty ty {
			get;
			set;
		}

		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			if (engine.operands.peek ().get_type () != ty.get_real_type ()) {
				throw new EvaluationError.TYPE_MISMATCH (@"Type is not as requested. $(source.source):$(source.line):$(source.offset)");
			}
		}
		public override Expression transform () {
			expression = expression.transform (); return this;
		}
	}
	public Data.Datum convert (ExecutionEngine engine, Expression expression, Data.Ty ty) throws EvaluationError {
		engine.call (expression);
		var result = engine.operands.pop ();
		var result_type = result.get_type ();
		if (result_type == ty.get_real_type ()) {
			return result;
		}

		switch (ty) {
		 case Data.Ty.FLOAT:
			 if (result_type == typeof (Data.Integer)) {
				 return new Data.Float (((Data.Integer)result).@value);
			 }
			 break;

		 case Data.Ty.INT:
			 if (result_type == typeof (Data.Float)) {
				 return new Data.Integer ((int) ((Data.Float)result).@value);
			 }
			 break;

		 case Data.Ty.STR:
			 if (result_type == typeof (Data.Boolean)) {
				 return new Data.String (((Data.Boolean)result).@value.to_string ());
			 }
			 if (result_type == typeof (Data.Integer)) {
				 return new Data.String (((Data.Integer)result).@value.to_string ());
			 }
			 if (result_type == typeof (Data.Float)) {
				 return new Data.String (((Data.Float)result).@value.to_string ());
			 }
			 break;
		}
		throw new EvaluationError.TYPE_MISMATCH (@"Type cannot be coerced as requested. $(expression.source.source):$(expression.source.line):$(expression.source.offset)");
	}
	internal class Coerce : Expression {
		public Expression expression {
			get;
			set;
		}
		public Data.Ty ty {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (convert (engine, expression, ty));
		}
		public override Expression transform () {
			expression = expression.transform (); return this;
		}
	}
}
