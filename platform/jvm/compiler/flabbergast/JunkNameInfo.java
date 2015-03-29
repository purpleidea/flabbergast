package flabbergast;

class JunkInfo extends NameInfo {
	public JunkInfo() {
	}

	@Override
	public void createChild(ErrorCollector collector, String name, String root,
			Ptr<Boolean> success) {
		children.put(name, new JunkInfo());
	}

	@Override
	public TypeSet ensureType(ErrorCollector collector, TypeSet type,
			Ptr<Boolean> success, boolean must_unbox) {
		return new TypeSet(TypeSet.ALL);
	}

	@Override
	public LoadableCache load(Generator generator,
			LoadableValue source_reference, LoadableValue context) {
		throw new IllegalAccessError("Attempted to load invalid name.");
	}
}