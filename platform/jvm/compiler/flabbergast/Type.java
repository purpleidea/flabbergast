package flabbergast;

public enum Type {
	Bool ( 1, Boolean.class),
	Float (2, Double.class),
	Frame ( 4, Frame.class),
	Int (8, Long.class),
	Str (16, Stringish.class),
	Template (32, Template.class),
	Unit ( 64, Unit.class);
	private int flag;
	private Class<?>clazz;
	Type(int flag, Class<?>clazz){ this.flag = flag; this.clazz = clazz; }
	int get(){ return flag; }
	TypeSet toSet(){return new TypeSet(this); }
	public Class<?> getRealClass() {
		
		return clazz;
	}
}
