package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

public interface UriLoader {
  int getPriority();

  String getUriName();

  Class<? extends Future> resolveUri(String uri, Ptr<LibraryFailure> reason);
}
