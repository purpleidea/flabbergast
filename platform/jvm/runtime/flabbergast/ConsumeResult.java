package flabbergast;

/**
 * Delegate for the callback from a computation.
 */
public interface ConsumeResult {
	void consume(Object result);
}