package flabbergast;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** Iterate over the keys of several of frames and templates. */
public class MergeIterator {
  private Map.Entry<String, Integer> current;

  private final SortedMap<String, Integer> dispatchers = new TreeMap<String, Integer>();

  private final int exit_dispatcher;

  private Iterator<Map.Entry<String, Integer>> iterator;

  private long position;

  public MergeIterator(Iterable<String>[] inputs, int default_dispatcher, int exit_dispatcher) {
    this.exit_dispatcher = exit_dispatcher;
    for (Iterable<String> input : inputs) {
      for (String key : input) {
        dispatchers.put(key, default_dispatcher);
      }
    }
  }

  /**
   * Add dispatcher for a particular key name.
   *
   * <p>When the key is encoutered, this dispatcher will be returned, instead of the default
   * dispatcher in the constructor. Added dispatchers are always invoked, even if they do not occur
   * in the input key space.
   */
  public void addDispatcher(String name, int dispatcher) {
    dispatchers.put(name, dispatcher);
  }

  /** The current attribute name. */
  public String getCurrent() {
    return current.getKey();
  }

  public Stringish getCurrentish() {
    return new SimpleStringish(current.getKey());
  }

  /** The current attribute ordinal, 1-based per the language spec. */
  public long getPosition() {
    return position;
  }

  public int next() {
    if (iterator == null) {
      iterator = dispatchers.entrySet().iterator();
    }
    if (iterator.hasNext()) {
      current = iterator.next();
      position++;
      return current.getValue();
    }
    return exit_dispatcher;
  }
}
