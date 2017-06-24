package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

public class UriInstantiator implements UriHandler {

  private UriLoader loader;

  public UriInstantiator(UriLoader loader) {
    this.loader = loader;
  }

  @Override
  public int getPriority() {
    return loader.getPriority();
  }

  @Override
  public String getUriName() {
    return loader.getUriName();
  }

  @Override
  public Future resolveUri(TaskMaster task_master, String uri, Ptr<LibraryFailure> reason) {
    Class<? extends Future> t = loader.resolveUri(uri, reason);
    if (t == null) {
      return null;
    }

    try {
      Future computation;
      computation = t.getDeclaredConstructor(TaskMaster.class).newInstance(task_master);
      return computation;
    } catch (Exception e) {
      reason.set(LibraryFailure.CORRUPT);
      return null;
    }
  }
}
