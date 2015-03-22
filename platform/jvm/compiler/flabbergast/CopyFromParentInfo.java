package flabbergast;

class CopyFromParentInfo extends NameInfo {
	private Environment environment;
	boolean force_back;
	TypeSet mask = new TypeSet();
	private boolean must_unbox = false;
	private NameInfo source;

	public CopyFromParentInfo(Environment environment, String name,
			NameInfo source, boolean force_back) {
		this.environment = environment;
		this.name = name;
		this.source = source;
		this.force_back = force_back;
	}

	@Override
	public String checkValidNarrowing(LookupCache next, LookupCache current) {
		if (current.has(source)) {
			LoadableValue parent_value = current.get(source);
			Type union_type = Type.fromNative(parent_value.getBackingType());
			if (mask.contains(union_type)) {
				return String.format(
						"Value for “%s” must be to %s, but it is %s.", name,
						mask, union_type);
			} else {
				next.set(this, parent_value);
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public void createChild(ErrorCollector collector, String name, String root,
			Ptr<Boolean> success) {
		if (force_back) {
			source.createChild(collector, name, root, success);
		}
		if (source.hasName(name)) {
			children.put(
					name,
					new CopyFromParentInfo(environment, root + "." + name,
							source.lookup(collector, name, success), force_back));
		} else {
			children.put(name, new OpenNameInfo(environment, root + "." + name));
		}
	}

	@Override
	public TypeSet ensureType(ErrorCollector collector, TypeSet type,
			Ptr<Boolean> success, boolean must_unbox) {
		this.must_unbox |= must_unbox;
		if (force_back) {
			return source.ensureType(collector, type, success, must_unbox);
		} else {
			if (!mask.restrict(type)) {
				success.set(false);
				collector.reportLookupTypeError(environment, name, mask, type);
			}
			return mask;
		}
	}

	@Override
	public boolean hasName(String name) {
		return super.hasName(name) || source.hasName(name);
	}

	@Override
	public LoadableCache load(Generator generator,
			LoadableValue source_reference, LoadableValue context)
			throws NoSuchMethodException, SecurityException {
		LoadableCache source_cache = source.load(generator, source_reference,
				context);
		return new LoadableCache(source_cache.getValue(), source_cache
				.getPossibleTypes().intersect(mask), this, must_unbox);
	}

	@Override
	public boolean needsLoad(LookupCache current) {
		return !current.has(source) || must_unbox
				&& current.get(source).getBackingType() == Object.class;
	}
}
