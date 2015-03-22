package flabbergast;

public enum Type {
	Bool(1, boolean.class, Boolean.class), Float(2, double.class, Double.class,
			Float.class, float.class), Frame(4, Frame.class), Int(8,
			long.class, Long.class, Integer.class, int.class, Short.class,
			short.class, Byte.class, byte.class), Str(16, Stringish.class,
			String.class), Template(32, Template.class), Unit(64, Unit.class);
	public static Type fromNative(Class<?> clazz) {
		for (Type t : values()) {
			for (Class<?> c : t.clazz)
				if (c == clazz)
					return t;
		}
		return null;
	}

	private Class<?>[] clazz;

	private int flag;

	Type(int flag, Class<?>... clazz) {
		this.flag = flag;
		this.clazz = clazz;
	}

	int get() {
		return flag;
	}

	public Class<?> getRealClass() {

		return clazz[0];
	}

	TypeSet toSet() {
		return new TypeSet(this);
	}
}
