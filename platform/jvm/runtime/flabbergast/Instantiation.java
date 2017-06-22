package flabbergast;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Instantiation extends InterlockedLookup {
    private String[] names;
    private Map<String, Object> overrides = new HashMap<String, Object>();
    private Frame container;
    private Template tmpl;
    public Instantiation(TaskMaster task_master, SourceReference source_reference,
                         Context context, Frame container, String... names) {
        super(task_master, source_reference, context);
        this.container = container;
        this.names = names;
    }

    public void add(String name, Object val) {
        overrides.put(name, val);
    }

    @Override
    protected void setup() {
        Sink<Template> tmpl_lookup = find(Template.class, x-> tmpl = x);
        tmpl_lookup.allowDefault(false, null);
        tmpl_lookup.lookup(names);
    }
    @Override
    protected void resolve() {
        MutableFrame frame = new MutableFrame(task_master,
                                              new JunctionReference("instantiation", "<native>", 0, 0, 0, 0,
                                                      source_reference, tmpl.getSourceReference()), Context.append(
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
    }
}
