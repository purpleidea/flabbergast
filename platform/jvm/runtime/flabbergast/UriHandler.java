package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

public interface UriHandler {
    String getUriName();

    int getPriority();

    Computation resolveUri(TaskMaster task_master, String uri,
                           Ptr<LibraryFailure> reason);
}
