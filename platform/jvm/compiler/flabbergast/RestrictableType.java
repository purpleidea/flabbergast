package flabbergast;

abstract class RestrictableType extends NameInfo {
	public abstract TypeSet getRestrictedType();

	public abstract boolean mustUnbox();
}
