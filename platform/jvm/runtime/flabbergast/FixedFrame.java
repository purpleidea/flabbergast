package flabbergast;

import java.util.Iterator;
import java.util.TreeMap;

public class FixedFrame extends Frame {

  private TreeMap<String, Object> attributes = new TreeMap<String, Object>();

  public FixedFrame(String id, SourceReference source_ref) {
    super(id, source_ref, null, null);
  }

  public FixedFrame add(Frame[] frames) {
    for (Frame frame : frames) {
      attributes.put(frame.getId().toString(), frame);
    }
    return this;
  }

  public void add(String name, byte[] value) {
    attributes.put(name, value);
  }

  public void add(String name, long value) {
    attributes.put(name, value);
  }

  public void add(String name, String value) {
    attributes.put(name, new SimpleStringish(value));
  }

  @Override
  public int count() {
    return attributes.size();
  }

  @Override
  public Object get(String name) {
    return attributes.containsKey(name) ? attributes.get(name) : null;
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
}
