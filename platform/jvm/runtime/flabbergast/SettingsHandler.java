package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;
import java.net.URLDecoder;

public class SettingsHandler implements UriHandler {
  public static final SettingsHandler INSTANCE = new SettingsHandler();

  private SettingsHandler() {}

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public String getUriName() {
    return "VM-specific settings";
  }

  @Override
  public Future resolveUri(TaskMaster task_master, String uri, Ptr<LibraryFailure> reason) {

    if (!uri.startsWith("settings:")) return null;
    try {
      String value = System.getProperty(URLDecoder.decode(uri.substring(9), "UTF-8"));
      return new Precomputation(value == null ? Unit.NULL : new SimpleStringish(value));
    } catch (Exception e) {
      return new FailureFuture(task_master, new NativeSourceReference(uri), e.getMessage());
    }
  }
}
