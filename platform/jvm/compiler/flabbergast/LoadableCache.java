package flabbergast;

import java.util.Collection;

class LoadableCache {
	private NameInfo name_info;
	private TypeSet possible_types;
	private LoadableValue value;
	public LoadableValue getValue (){ return value; }
	public TypeSet getPossibleTypes (){ return possible_types; }
	public NameInfo getNameInfo() { return name_info; }
	public boolean getSinglyTyped(){  return types.size() == 1; }
	private boolean needs_unbox;
	private Collection<Class<?>> types; 
	public Collection<Class<?>> getTypes (){ return types; }
	public boolean getNeedsUnbox (){ return needs_unbox; }
	public boolean getDirectCopy (){ 
		return value.getBackingType() != Object.class || !needs_unbox;
	}
	public LoadableCache(LoadableValue loadable_value, RestrictableType name_info) {
		this(loadable_value, name_info.getRestrictedType(), name_info, name_info.mustUnbox());
	
	}
	public LoadableCache(LoadableValue loadable_value, TypeSet type, NameInfo name_info, boolean must_unbox) {
		this.value = loadable_value;
		this.possible_types = type;
		this.name_info = name_info;
		this.types = type.getClasses();
		this.needs_unbox = must_unbox;
	}
}
