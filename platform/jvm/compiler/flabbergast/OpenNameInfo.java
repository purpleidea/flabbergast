package flabbergast;

class OpenNameInfo extends NameInfo {
  private Environment environment;
  private boolean must_unbox = false;
  private TypeSet real_type = new TypeSet(TypeSet.ALL);

  public OpenNameInfo(Environment environment, String name) {
    this.environment = environment;
    this.name = name;
  }

  @Override
  public void collectUses(ApiGenerator apigen) {
    apigen.registerUse(getName());
    for (NameInfo child : children.values()) {
      child.collectUses(apigen);
    }
  }

  @Override
  public void createChild(
      ErrorCollector collector, String name, String root, Ptr<Boolean> success) {
    children.put(name, new OpenNameInfo(environment, root + "." + name));
  }

  @Override
  public TypeSet ensureType(
      ErrorCollector collector, TypeSet type, Ptr<Boolean> success, boolean must_unbox) {
    this.must_unbox |= must_unbox;
    if (!real_type.restrict(type)) {
      success.set(false);
      collector.reportLookupTypeError(environment, name, real_type, type);
    }
    return real_type;
  }

  @Override
  public LoadableCache load(
      Generator generator, LoadableValue source_reference, LoadableValue context) throws Exception {
    return new LoadableCache(
        generateLookupField(generator, source_reference, context), real_type, this, must_unbox);
  }

  @Override
  public boolean needsLoad(LookupCache current) {
    return true;
  }
}
