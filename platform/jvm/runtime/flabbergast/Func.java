package flabbergast;

interface Func<T, R> {
  public R invoke(T arg) throws Exception;
}
