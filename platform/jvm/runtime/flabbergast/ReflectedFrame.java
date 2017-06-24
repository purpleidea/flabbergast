package flabbergast;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/** A Frame wrapper over a Java object. */
public class ReflectedFrame extends Frame {

  public interface Transform<T> {
    Object invoke(T src);
  }

  public static <T> ReflectedFrame create(
      String id, T backing, Map<String, Transform<T>> accessors) {
    TreeMap<String, Object> attributes = new TreeMap<String, Object>();
    for (Entry<String, Transform<T>> entry : accessors.entrySet()) {
      Object result = entry.getValue().invoke(backing);
      if (result == null) {
        result = Unit.NULL;
      } else if (result instanceof Boolean
          || result instanceof Double
          || result instanceof Long
          || result instanceof Frame
          || result instanceof Stringish
          || result instanceof Template
          || result instanceof Unit) {
      } else if (result instanceof String) {
        result = new SimpleStringish((String) result);
      } else {
        throw new ClassCastException(
            "Value for "
                + entry.getKey()
                + " is non-Flabbergast type "
                + result.getClass().getSimpleName()
                + ".");
      }
      attributes.put(entry.getKey(), result);
    }
    return new ReflectedFrame(id, new JavaSourceReference(), backing, attributes);
  }

  public static <T> ReflectedFrame create(
      TaskMaster task_master, T backing, Map<String, Transform<T>> accessors) {
    return create(SupportFunctions.ordinalNameStr(task_master.nextId()), backing, accessors);
  }

  private final TreeMap<String, Object> attributes;

  private final Object backing;

  private ReflectedFrame(
      String id, SourceReference source_ref, Object backing, TreeMap<String, Object> attributes) {
    super(id, source_ref, null, null);
    this.backing = backing;
    this.attributes = attributes;
  }

  @Override
  public int count() {
    return attributes.size();
  }

  /**
   * Access the functions in the frames. Frames should not be mutated, but this policy is not
   * enforced by this class; it must be done in the calling code.
   */
  @Override
  public Object get(String name) {
    return attributes.containsKey(name) ? attributes.get(name) : null;
  }

  public Object getBacking() {
    return backing;
  }

  /** Check if an attribute name is present in the frame. */
  @Override
  public boolean has(String name) {
    return attributes.containsKey(name);
  }

  @Override
  public Iterator<String> iterator() {
    return attributes.keySet().iterator();
  }

  public void set(String name, Object value) {
    if (attributes.containsKey(name)) {
      throw new IllegalArgumentException();
    }
    attributes.put(name, value);
  }
}
