package flabbergast.util;

/**
 * A three argument function that can throw an exception
 *
 * @param <T1> the first input argument type
 * @param <T2> the second input argument type
 * @param <T3> the third input argument type
 * @param <R> the return type
 */
public interface WhinyFunction3<T1, T2, T3, R> {
  /** Call the function */
  R apply(T1 arg1, T2 arg2, T3 arg3) throws Exception;

  /**
   * Create a function where the second, and third arguments are already bound to the values
   * provided
   */
  default WhinyFunction<T1, R> tailBind(T2 arg2, T3 arg3) {
    return arg1 -> apply(arg1, arg2, arg3);
  }
}
