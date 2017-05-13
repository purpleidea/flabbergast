package flabbergast.util;

/**
 * A four argument function that can throw an exception
 *
 * @param <T1> the first input argument type
 * @param <T2> the second input argument type
 * @param <T3> the third input argument type
 * @param <T4> the fourth input argument type
 * @param <R> the return type
 */
public interface WhinyFunction4<T1, T2, T3, T4, R> {
  /** Call the function */
  R apply(T1 arg1, T2 arg2, T3 arg3, T4 arg4) throws Exception;

  /**
   * Create a function where the second, third, and fourth arguments are already bound to the values
   * provided
   */
  default WhinyFunction<T1, R> tailBind(T2 arg2, T3 arg3, T4 arg4) {
    return arg1 -> apply(arg1, arg2, arg3, arg4);
  }
}
