package flabbergast;

public interface UriHandler {
	String getUriName();

	Class<? extends Computation> resolveUri(String uri, Ptr<Boolean> stop);
}
