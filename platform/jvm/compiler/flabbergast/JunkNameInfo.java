package flabbergast;


 class JunkInfo extends NameInfo {
	public JunkInfo() {
	}
	public TypeSet ensureType(ErrorCollector collector, TypeSet type, Ptr<Boolean> success, boolean must_unbox) {
		return new TypeSet();
	}
	public void createChild(ErrorCollector collector, String name, String root, Ptr<Boolean>success) {
		children.put(name, new JunkInfo());
	}
	public LoadableCache load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		throw new IllegalAccessError("Attempted to load invalid name.");
	}
}