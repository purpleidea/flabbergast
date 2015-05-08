package flabbergast;

class OverrideNameInfo extends RestrictableType {
	private Environment environment;
	private boolean must_unbox = false;
	private TypeSet real_type = new TypeSet(TypeSet.ALL);

	public OverrideNameInfo(Environment environment, String name) {
		this.environment = environment;
		this.name = name;
	}

	@Override
	public void createChild(ErrorCollector collector, String name, String root,
			Ptr<Boolean> success) {
		children.put(name, new OpenNameInfo(environment, root + "." + name));
	}

	@Override
	public TypeSet ensureType(ErrorCollector collector, TypeSet type,
			Ptr<Boolean> success, boolean must_unbox) {
		this.must_unbox |= must_unbox;
		if (!real_type.restrict(type)) {
			success.set(false);
			collector.reportLookupTypeError(environment, name, real_type, type);
		}
		return real_type;
	}

	@Override
	public TypeSet getRestrictedType() {
		return real_type;
	}

	@Override
	public LoadableCache load(Generator generator,
			LoadableValue source_reference, LoadableValue context) {
		return new LoadableCache(
				((FunctionGenerator) generator).getInitialOriginal(),
				real_type, this, must_unbox);
	}

	@Override
	public boolean mustUnbox() {
		return must_unbox;
	}
}
