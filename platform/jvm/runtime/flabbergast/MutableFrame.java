package flabbergast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * A Frame in the Flabbergast language.
 */
public class MutableFrame extends Frame {

    private TreeMap<String, Object> attributes = new TreeMap<String, Object>();

    private ArrayList<Computation> unslotted = new ArrayList<Computation>();

    protected final TaskMaster task_master;

    public MutableFrame(TaskMaster task_master, SourceReference source_ref,
                        Context context, Frame container) {
        super(task_master, source_ref, context, container);
        this.task_master = task_master;
    }

    @Override
    public int count() {
        return attributes.size();
    }

    /**
     * Access the functions in the frames. Frames should not be mutated, but
     * this policy is not enforced by this class; it must be done in the calling
     * code.
     */
    @Override
    public Object get(String name) {
        // If this frame is being looked at, then all its pending attributes
        // should
        // be slotted.
        slot();
        return attributes.containsKey(name) ? attributes.get(name) : null;
    }

    /**
     * Check if an attribute name is present in the frame.
     */
    @Override
    public boolean has(String name) {
        return attributes.containsKey(name);
    }

    @Override
    public Iterator<String> iterator() {
        return attributes.keySet().iterator();
    }

    public void set(long ordinal, Object value) {
        set(TaskMaster.ordinalNameStr(ordinal), value);
    }
    public void set(final String name, Object value) {
        if (value == null) {
            return;
        }
        if (attributes.containsKey(name)) {
            throw new IllegalStateException("Redefinition of attribute " + name
                                            + ".");
        }
        if (value instanceof ComputeValue) {
            Computation computation = ((ComputeValue) value).invoke(
                                          task_master, getSourceReference(), getContext(), this,
                                          getContainer());
            attributes.put(name, computation);
            /*
             * When this computation has completed, replace its value in the
             * frame.
             */
            computation.listenDelayed(new ConsumeResult() {
                @Override
                public void consume(Object result) {
                    attributes.put(name, result);
                }
            });
            /*
             * If the value is a computation, it cannot be slotted for execution
             * since it might depend on lookups that reference this frame.
             * Therefore, put it in a queue for later activation.
             */
            unslotted.add(computation);
        } else {
            if (value instanceof MutableFrame) {
                /*
                 * If the value added is a frame, it might be in a complicated
                 * slotting arrangement. The safest thing to do is to steal its
                 * unslotted children and slot them when we are slotted (or
                 * absorbed into another frame.
                 */

                MutableFrame other = (MutableFrame) value;
                unslotted.addAll(other.unslotted);
                other.unslotted = unslotted;
            }
            attributes.put(name, value);
        }
    }

    /**
     * Trigger any unfinished computations contained in this frame to be
     * executed.
     *
     * When a frame is being filled, unfinished computations may be added. They
     * cannot be started immediately, since the frame may still have members to
     * be added and those changes will be visible to the lookup environments of
     * those computations. Only when a frame is “returned” can the computations
     * be started. This should be called before returning to trigger
     * computation.
     */
    public void slot() {
        for (Computation computation : unslotted) {
            computation.slot();
        }
        unslotted.clear();
    }
}
