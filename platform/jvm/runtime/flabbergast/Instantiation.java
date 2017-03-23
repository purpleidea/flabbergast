package flabbergast;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Instantiation extends Future {
    private String[] names;
    private Map<String, Object> overrides = new HashMap<String, Object>();
    private Context context;
    private Frame container;
    private Template tmpl;
    private SourceReference src_ref;
    private InterlockedLookup interlock;
    public Instantiation(TaskMaster task_master, SourceReference src_ref,
                         Context context, Frame container, String... names) {
        super(task_master);
        this.src_ref = src_ref;
        this.context = context;
        this.container = container;
        this.names = names;
    }

    public void add(String name, Object val) {
        overrides.put(name, val);
    }

    @Override
    protected void run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, src_ref, context);
            interlock.lookup(Template.class, x-> tmpl = x, names);
        }
        if (!interlock.away()) return;
        MutableFrame frame = new MutableFrame(task_master,
                                              new JunctionReference("instantiation", "<native>", 0, 0, 0, 0,
                                                      src_ref, tmpl.getSourceReference()), Context.append(
                                                              context, tmpl.getContext()), container);
        for (Entry<String, Object> entry : overrides.entrySet()) {
            frame.set(entry.getKey(), entry.getValue());
        }
        for (String name : tmpl) {
            if (!overrides.containsKey(name)) {
                frame.set(name, tmpl.get(name));
            }
        }
        result = frame;
        return;
    }
}
