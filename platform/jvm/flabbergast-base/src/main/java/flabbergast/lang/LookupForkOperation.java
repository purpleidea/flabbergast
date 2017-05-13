package flabbergast.lang;

import java.util.stream.Stream;

/** A handler for the output of a step in a lookup exploration operation */
public interface LookupForkOperation extends LookupNextOperation {

  /**
   * Finish this step exploring multiple possibilities
   *
   * @param values the values to consider; in order to explore
   * @see #finish(Any)
   */
  void fork(Stream<Any> values);

  void forkPromises(Stream<Promise<Any>> values);
}
