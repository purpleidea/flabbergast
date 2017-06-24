package flabbergast;

interface TypeableElement {
  TypeSet ensureType(
      ErrorCollector collector, TypeSet type, Ptr<Boolean> success, boolean must_unbox);
}
