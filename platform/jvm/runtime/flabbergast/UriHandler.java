package flabbergast;

public interface UriHandler {
	String getUriName();

	Class<? extends Computation> ResolveUri(String uri, Ptr<Boolean> stop);
}
