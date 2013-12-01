namespace Flabbergast.Expressions {
	internal class TypeCheck : Expression {
		public Expression expression {
			get;
			set;
		}
		public Ty ty {
			get;
			set;
		}

		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			var result = engine.operands.pop ();
			engine.operands.push (new Boolean (get_datum_type (result) == ty.get_real_type ()));
		}
	}
	internal class TypeEnsure : Expression {
		public Expression expression {
			get;
			set;
		}
		public Ty ty {
			get;
			set;
		}

		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call (expression);
			if (get_datum_type (engine.operands.peek ()) != ty.get_real_type ()) {
				throw new EvaluationError.TYPE_MISMATCH ("Type is not as requested.");
			}
		}
	}
	public Datum convert(ExecutionEngine engine, Expression expression, Ty ty) throws EvaluationError {
		engine.call (expression);
		var result = engine.operands.pop ();
		var result_type = get_datum_type (result);
		if (result_type == ty.get_real_type ()) {
			return result;
		}

		switch (ty) {
		 case Ty.FLOAT:
			 if (result_type == typeof (Integer)) {
				 return new Float (((Integer) result).@value);
			 }
			 break;

		 case Ty.INT:
			 if (result_type == typeof (Float)) {
				 return new Integer ((int) ((Float) result).@value);
			 }
			 break;

		 case Ty.STR:
			 if (result_type == typeof (Boolean)) {
				 return new String (((Boolean) result).@value.to_string ());
			 }
			 if (result_type == typeof (Integer)) {
				 return new String (((Integer) result).@value.to_string ());
			 }
			 if (result_type == typeof (Float)) {
				 return new String (((Float) result).@value.to_string ());
			 }
			 break;
		}
		throw new EvaluationError.TYPE_MISMATCH ("Type cannot be coerced as requested.");
	}
	internal class Coerce : Expression {
		public Expression expression {
			get;
			set;
		}
		public Ty ty {
			get;
			set;
		}
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.operands.push (convert (engine, expression, ty));
		}
	}
}