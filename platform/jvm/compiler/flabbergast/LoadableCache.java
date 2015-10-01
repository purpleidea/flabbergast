package flabbergast;

import java.util.List;

class LoadableCache {
    private NameInfo name_info;
    private boolean needs_unbox;
    private TypeSet possible_types;

    private List<Class<Object>> types;

    private LoadableValue value;

    public LoadableCache(LoadableValue loadable_value,
                         RestrictableType name_info) {
        this(loadable_value, name_info.getRestrictedType(), name_info,
             name_info.mustUnbox());

    }

    public LoadableCache(LoadableValue loadable_value, TypeSet type,
                         NameInfo name_info, boolean must_unbox) {
        this.value = loadable_value;
        this.possible_types = type;
        this.name_info = name_info;
        this.types = type.getClasses();
        this.needs_unbox = must_unbox;
    }

    public boolean getDirectCopy() {
        return value.getBackingType() != Object.class || !needs_unbox;
    }

    public NameInfo getNameInfo() {
        return name_info;
    }

    public boolean getNeedsUnbox() {
        return needs_unbox;
    }

    public TypeSet getPossibleTypes() {
        return possible_types;
    }

    public boolean getSinglyTyped() {
        return types.size() == 1;
    }

    public List<Class<Object>> getTypes() {
        return types;
    }

    public LoadableValue getValue() {
        return value;
    }
}
