package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

public interface UriHandler {
	String getUriName();

	Class<? extends Computation> resolveUri(String uri,
			Ptr<LibraryFailure> reason);
}
