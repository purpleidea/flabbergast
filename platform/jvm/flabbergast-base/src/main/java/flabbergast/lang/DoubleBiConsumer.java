package flabbergast.lang;

/** Dispatch helper to handle floating point values */
public abstract class DoubleBiConsumer {

  /** Create a new consumer */
  public DoubleBiConsumer() {}
  /** Process two floating point values */
  protected abstract void accept(double left, double right);

  /**
   * Dispatch an floating point value and another value which must be either integral and floating
   * point, which will be upgraded to floating point if integral.
   */
  public final void dispatch(
      Future<?> future, SourceReference sourceReference, double leftValue, Any right) {
    future.reschedule(
        () ->
            right.accept(
                new WhinyAnyConsumer() {
                  @Override
                  public void accept(double rightValue) {
                    DoubleBiConsumer.this.accept(leftValue, rightValue);
                  }

                  @Override
                  public void accept(long rightValue) {
                    DoubleBiConsumer.this.accept(leftValue, rightValue);
                  }

                  @Override
                  protected void fail(String type) {
                    future.error(
                        sourceReference, String.format("Expected Float or Int, but got %s.", type));
                  }
                }));
  }
}
