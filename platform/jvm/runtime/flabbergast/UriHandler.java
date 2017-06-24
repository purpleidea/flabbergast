package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

public interface UriHandler {
  int getPriority();

  String getUriName();

  Future resolveUri(TaskMaster task_master, String uri, Ptr<LibraryFailure> reason);
}
