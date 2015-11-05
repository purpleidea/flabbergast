package flabbergast;

import java.util.LinkedList;
import java.util.List;

/**
 * Do lookup by creating a grid of contexts where the value might reside and all
 * the needed names.
 */
public class Lookup extends Computation {
    public static class DoLookup implements ComputeValue {
        private final String[] names;
        public DoLookup(String... names) {
            this.names = names;
        }

        public Computation invoke(TaskMaster task_master,
                                  SourceReference source_reference, Context context, Frame self,
                                  Frame container) {
            if (names.length == 0) {
                return new FailureComputation(task_master, source_reference, "Missing names in lookup.");
            }
            for (String name : names) {
                if (!task_master.verifySymbol(source_reference, name)) {
                    return BlackholeComputation.INSTANCE;
                }
            }
            return new Lookup(task_master, source_reference, names, context);
        }
    }
    private class Attempt implements ConsumeResult {
        int frame;
        int name;
        Frame result_frame;
        Frame source_frame;

        public Attempt(int name, int frame, Frame source_frame) {
            this.name = name;
            this.frame = frame;
            this.source_frame = source_frame;
        }

        @Override
        public void consume(Object return_value) {
            if (name == names.length - 1) {
                result = return_value;
                wakeupListeners();
            } else if (return_value instanceof Frame) {
                result_frame = ((Frame) return_value);
                Attempt next = new Attempt(name + 1, frame, result_frame);
                known_attempts.add(next);
                if (result_frame.getOrSubscribe(names[name + 1], next)) {
                    return;
                }
                activateNext();
            } else {
                task_master.reportLookupError(Lookup.this,
                                              return_value.getClass());
            }
        }

    }

    private int frame_index = 0;

    private final Frame[] frames;

    private LinkedList<Attempt> known_attempts = new LinkedList<Attempt>();

    /**
     * The name components in the lookup expression.
     */
    private String[] names;
    private SourceReference source_reference;

    public Lookup(TaskMaster task_master, SourceReference source_ref,
                  String[] names, Context context) {
        super(task_master);
        this.source_reference = source_ref;
        this.names = names;
        frames = new Frame[context.getLength()];
        int frame_index = 0;
        for (Frame frame : context) {
            frames[frame_index] = frame;
            frame_index++;
        }
    }

    private void activateNext() {
        while (frame_index < frames.length) {
            int index = frame_index++;
            Attempt root_attempt = new Attempt(0, index, frames[index]);
            known_attempts.add(root_attempt);
            if (frames[index].getOrSubscribe(names[0], root_attempt)) {
                return;
            }
        }
        task_master.reportLookupError(this, null);
    }

    public Frame get(int name, int frame) {
        for (int index = 0; index < known_attempts.size(); index++) {
            Attempt current = known_attempts.get(index);
            if (current.frame == frame && current.name > name
                    || current.frame > frame) {
                return null;
            }
            if (current.frame == frame && current.name == name) {
                return current.source_frame;
            }
        }
        return null;
    }

    public int getFrameCount() {
        return frames.length;
    }

    public Frame getLastFrame() {
        return known_attempts.getLast().source_frame;
    }

    public String getLastName() {
        return names[known_attempts.getLast().name];
    }

    public String getName() {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < names.length; n++) {
            if (n > 0) {
                sb.append(".");
            }
            sb.append(names[n]);
        }
        return sb.toString();
    }

    public String getName(int index) {
        return names[index];
    }

    public int getNameCount() {
        return names.length;
    }

    public SourceReference getSourceReference() {
        return source_reference;
    }

    @Override
    protected void run() {
        activateNext();

    }

}
