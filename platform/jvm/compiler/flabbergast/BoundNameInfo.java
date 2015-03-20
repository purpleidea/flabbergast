package flabbergast;


class BoundNameInfo extends RestrictableType {
	private Environment environment;
	private TypeableElement target;
	private boolean must_unbox = false;
	public TypeSet getRestrictedType (){ return restricted_type; }
	public boolean mustUnbox() { return must_unbox; }
	private TypeSet restricted_type = new TypeSet();
	public BoundNameInfo(Environment environment, String name, TypeableElement target) {
		this.environment = environment;
		this.name = name;
		this.target = target;
	}
	public TypeSet ensureType(ErrorCollector collector, TypeSet type, Ptr<Boolean>success, boolean must_unbox) {
		this.must_unbox |= must_unbox;
		restricted_type.restrict(type);
		return target.ensureType(collector, type, success, must_unbox);
	}
	public boolean needsLoad(LookupCache current) {
		return !current.has(this);
	}
	public void createChild(ErrorCollector collector, String name, String root, Ptr<Boolean> success) {
		children.put(name, new OpenNameInfo(environment, root + "." + name));
	}
	public LoadableCache load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		return new LoadableCache(generateLookupField(generator, source_reference, context), restricted_type, this, must_unbox);
	}
}
