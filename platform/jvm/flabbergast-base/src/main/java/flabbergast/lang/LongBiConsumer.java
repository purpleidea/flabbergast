package flabbergast.lang;

/** Dispatch helper to consume binary numeric operations */
public abstract class LongBiConsumer extends DoubleBiConsumer {
  /** Create an instance of a consumer */
  public LongBiConsumer() {}
  /** Handle a case of two integral values */
  protected abstract void accept(long left, long right);

  /**
   * Dispatch two values which must be some combination of integral and floating point, which will
   * be upgraded to floating point if mixed.
   */
  public final void dispatch(
      Future<?> future, SourceReference sourceReference, Any left, Any right) {
    future.reschedule(
        () ->
            left.accept(
                new WhinyAnyConsumer() {

                  @Override
                  public void accept(double leftValue) {
                    dispatch(future, sourceReference, leftValue, right);
                  }

                  @Override
                  public void accept(long leftValue) {
                    dispatch(future, sourceReference, leftValue, right);
                  }

                  @Override
                  protected void fail(String type) {
                    future.error(
                        sourceReference, String.format("Expected Float or Int, but got %s.", type));
                  }
                }));
  }

  /**
   * Dispatch an integral value and another value which must be either integral and floating point.
   * The provided value will be upgraded to floating point if mixed.
   */
  public final void dispatch(
      Future<?> future, SourceReference sourceReference, long leftValue, Any right) {
    future.reschedule(
        () ->
            right.accept(
                new WhinyAnyConsumer() {

                  @Override
                  public void accept(double rightValue) {
                    LongBiConsumer.this.accept(leftValue, rightValue);
                  }

                  @Override
                  public void accept(long rightValue) {
                    LongBiConsumer.this.accept(leftValue, rightValue);
                  }

                  @Override
                  protected void fail(String type) {
                    future.error(
                        sourceReference, String.format("Expected Float or Int, but got %s.", type));
                  }
                }));
  }
}
