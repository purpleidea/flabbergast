package flabbergast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** A Flabbergast Template, holding functions for computing attributes. */
public class Template implements Iterable<String> {
  private Map<String, ComputeValue> attributes = new HashMap<String, ComputeValue>();

  private Frame container;

  private Context context;

  private SourceReference source_reference;

  public Template(SourceReference source_ref, Context context, Frame container) {
    this.source_reference = source_ref;
    this.context = context;
    this.container = container;
  }

  /**
   * Access the functions in the template. Templates should not be mutated, but this policy is not
   * enforced by this class; it must be done in the calling code.
   */
  public ComputeValue get(String name) {
    return attributes.containsKey(name) ? attributes.get(name) : null;
  }

  public ComputeValue get(Stringish name) {
    return get(name.toString());
  }

  public Frame getContainer() {
    return container;
  }

  /** The context in which this template was created. */
  public Context getContext() {
    return context;
  }

  /** The stack trace at the time of creation. */
  public SourceReference getSourceReference() {
    return source_reference;
  }

  @Override
  public Iterator<String> iterator() {
    return attributes.keySet().iterator();
  }

  public void set(String name, ComputeValue value) {
    if (value == null) {
      return;
    }
    if (attributes.containsKey(name)) {
      throw new IllegalStateException("Redefinition of attribute " + name + ".");
    }
    attributes.put(name, value);
  }
}
