namespace Flabbergast {
	public interface Expression : Object {
		public abstract void evaluate(ExecutionEngine engine) throws EvaluationError;
	}
}