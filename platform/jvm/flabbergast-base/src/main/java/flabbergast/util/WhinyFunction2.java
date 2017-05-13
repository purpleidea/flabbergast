package flabbergast.util;

/**
 * A two argument function that can throw an exception
 *
 * @param <T1> the first input argument type
 * @param <T2> the second input argument type
 * @param <R> the return type
 */
public interface WhinyFunction2<T1, T2, R> {
  /** Call the function */
  R apply(T1 arg1, T2 arg2) throws Exception;

  /**
   * Create a function where the second argument is already bound to the value provided
   *
   * @param arg2 the value for the second argument
   */
  default WhinyFunction<T1, R> tailBind(T2 arg2) {
    return arg1 -> apply(arg1, arg2);
  }
}
