package flabbergast;

import java.util.ArrayList;
import java.util.List;

public class TypeSet {
	public static TypeSet ALL = new TypeSet(127);
	public static final TypeSet EMPTY = new TypeSet(0);

	public static TypeSet fromNative(Class<?> type) {
		if (type == Object.class)
			return ALL;
		Type t = Type.fromNative(type);
		if (t == null) {
			return EMPTY;
		}
		return new TypeSet(t);
	}

	private int flags = 0;

	private TypeSet(int flags) {
		this.flags = flags;
	}

	public TypeSet(Type... types) {
		for (Type type : types) {
			flags |= type.get();
		}
	}

	public TypeSet(TypeSet original, Type... additional) {
		this.flags = original.flags;
		for (Type t : additional) {
			this.flags |= t.get();
		}
	}

	public void add(TypeSet other) {
		flags |= other.flags;
	}

	public boolean contains(Type type) {
		return (type.get() & flags) != 0;
	}

	@SuppressWarnings("unchecked")
	public <T> List<Class<T>> getClasses() {
		List<Class<T>> classes = new ArrayList<Class<T>>();
		for (Type t : Type.values()) {
			if ((t.get() & flags) != 0) {
				classes.add((Class<T>) t.getRealClass());
			}
		}
		return classes;
	}

	public TypeSet horrendousMerge(TypeSet original) {
		/*
		 * This mostly revolves around null coalesence. Imagine we want to
		 * ensure the type of `Null ?? 3` is an integer. The alternate branch is
		 * just fine, so we let it alone. The main path is a problem. It can be
		 * integer or unit. If it's not in the original type mask (i.e., unit),
		 * that's okay, because we expect the coalescence expression to fix the
		 * problem at run-time.
		 */
		if (flags == 0)
			return this;
		if ((flags & original.flags) != 0) {
			this.flags &= original.flags;
		}
		return this;
	}

	public TypeSet intersect(TypeSet other) {
		return new TypeSet(flags & other.flags);
	}

	public boolean isEmpty() {
		return flags == 0;
	}

	public boolean restrict(TypeSet other) {
		int intersect = flags & other.flags;
		if (intersect == 0) {
			return false;
		} else {
			flags = intersect;
			return true;
		}
	}

	@Override
	public String toString() {
		if (flags == 0)
			return "<none>";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Type type : Type.values()) {
			if ((type.get() & flags) == 0)
				continue;
			if (first) {
				first = false;
			} else {
				sb.append(" or ");
			}
			sb.append(type.name());
		}
		return sb.toString();
	}

	public TypeSet union(Type type) {
		return new TypeSet(flags | type.get());
	}

	public TypeSet union(TypeSet other) {
		return new TypeSet(flags | other.flags);
	}

}
