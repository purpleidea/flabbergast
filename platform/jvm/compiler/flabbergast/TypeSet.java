package flabbergast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TypeSet {
	private int flags = 0;
	public static TypeSet ALL = new TypeSet(127);
	private TypeSet(int flags) {
		this.flags = flags;
	}
	public TypeSet(Type... types) {
		for(Type type : types) {
			flags|=type.get();
		}
	}
	public TypeSet intersect(TypeSet other) {
		return new TypeSet(flags & other.flags);
	}
	public TypeSet union(TypeSet other) {
		return new TypeSet(flags | other.flags);
	}
	public boolean restrict(TypeSet other) {
	int intersect = flags &other.flags;
	if (intersect == 0) {
		return false;
	} else {
		flags = intersect;
		return true;
	}
	}
	public  String toString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Type type : Type.values()) {
			if ((type.get() & flags) == 0)continue;
			if (first){
				first = false;
			} else {
				sb.append(type.name());
			}
			sb.append (" or ");
			sb.append(type.name());
		}
		if (first)
			return "<none>";
		return sb.toString();
	}
	public boolean isEmpty() {
		return flags == 0;
	}
	public Collection<Class<?>> getClasses() {
		List<Class<?>> classes= new ArrayList<Class<?>>();
		for(Type t : Type.values()) {
			if ((t.get() & flags) != 0) {
				classes.add(t.getRealClass());
			}
		}
		return classes;
	}
	public boolean contains(Type type) {
		return (type.get() & flags) != 0 ;
	}
	public TypeSet horrendousMerge(TypeSet original) {
		/* This mostly revolves around null coalesence. Imagine we want to ensure
		 * the type of `Null ?? 3` is an integer. The alternate branch is just fine, so
		 * we let it alone. The main path is a problem. It can be integer or unit. If
		 * it's not in the original type mask (i.e., unit), that's okay, because we
		 * expect the coalescence expression to fix the problem at run-time. */
		if (flags == 0)
			return this;
		return (flags& original.flags) == 0 ? original : this;
	}

}
