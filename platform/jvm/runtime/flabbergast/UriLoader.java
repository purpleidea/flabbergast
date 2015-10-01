package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

public interface UriLoader {
    String getUriName();

    Class<? extends Computation> resolveUri(String uri,
                                            Ptr<LibraryFailure> reason);
}
