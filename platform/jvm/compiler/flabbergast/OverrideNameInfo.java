package flabbergast;

class OverrideNameInfo extends RestrictableType {
	private Environment environment;
	private TypeSet real_type = new TypeSet();
	private boolean must_unbox = false;
	public TypeSet getRestrictedType() 
	{  return real_type; }
	public  boolean mustUnbox() { return must_unbox; }
	public OverrideNameInfo(Environment environment, String name) {
		this.environment = environment;
		this.name = name;
	}
	public TypeSet ensureType(ErrorCollector collector, TypeSet type, Ptr<Boolean> success, boolean must_unbox) {
		this.must_unbox |= must_unbox;
		if (!real_type.restrict(type)) {
			success.set(false);
			collector.reportLookupTypeError(environment, name, real_type, type);
		}
		return real_type;
	}
	public void createChild(ErrorCollector collector, String name, String root, Ptr<Boolean>success) {
		children.put(name, new OpenNameInfo(environment, root + "." + name));
	}
	public LoadableCache load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		return new LoadableCache(generator.InitialOriginal, real_type, this, must_unbox);
	}
}
