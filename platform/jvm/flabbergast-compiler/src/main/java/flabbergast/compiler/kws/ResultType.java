package flabbergast.compiler.kws;

/** The type of return associated with this a function */
public enum ResultType {
  /** A function that returns a single boxed value */
  ANY {
    @Override
    public KwsType returnType() {
      return KwsType.A;
    }
  },
  /** A function that returns a singled boxed value and an arbitrary number of attribute builders */
  ACCUMULATOR {
    @Override
    public KwsType returnType() {
      return KwsType.A;
    }
  },
  /** A function that returns a single fricassée chain */
  FRICASSEE {
    @Override
    public KwsType returnType() {
      return KwsType.E;
    }
  };

  /**
   * The return type for this method
   *
   * <p>This does not include the builder information
   */
  public abstract KwsType returnType();
}
