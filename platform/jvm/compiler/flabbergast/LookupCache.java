package flabbergast;

import java.util.HashMap;
import java.util.Map;

class LookupCache {
    private final Map<NameInfo, LoadableValue> defined_values = new HashMap<NameInfo, LoadableValue>();
    private final LookupCache parent;

    public LookupCache(LookupCache parent) {
        this.parent = parent;
    }

    public LoadableValue get(NameInfo name_info) {
        if (defined_values.containsKey(name_info)) {
            return defined_values.get(name_info);
        } else if (parent != null) {
            return parent.get(name_info);
        } else {
            throw new IllegalArgumentException(
                "Attempt to lookup cached name “"
                + name_info.getName()
                + "”, but it was never cached. This is a compiler bug.");
        }
    }

    public boolean has(NameInfo name_info) {
        if (defined_values.containsKey(name_info)) {
            return true;
        }
        return parent != null && parent.has(name_info);
    }

    public void set(NameInfo name_info, LoadableValue value) {
        defined_values.put(name_info, value);
    }
}
