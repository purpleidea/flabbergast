package flabbergast.lang;


/** Gets the value in a promise or an indication that it has not completed. */
public interface PromiseConsumer<T> {
  /** Use the value in a promise that has finished evaluation. */
  void accept(T value);
  /** This promise has not yet finished executing or encountered an error while executing. */
  void unfinished();
}
