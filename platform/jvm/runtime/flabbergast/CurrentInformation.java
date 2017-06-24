package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CurrentInformation implements UriHandler {

  private Map<String, Future> information = new HashMap<String, Future>();

  public CurrentInformation(boolean interactive) {
    String currentDirectory = ".";
    try {
      currentDirectory = new File(".").getCanonicalPath().toString();
    } catch (IOException e) {
    }
    information.put("interactive", new Precomputation(interactive));
    add("login", System.getProperty("user.name"));
    add("directory", currentDirectory);
    information.put("version", new Precomputation(Configuration.VERSION));
    add("machine/directory_separator", File.separator);
    add("machine/name", System.getProperty("os.name"));
    add("machine/path_separator", File.pathSeparator);
    add("machine/line_ending", String.format("%n"));
    add("vm/name", "JVM");
    add("vm/vendor", System.getProperty("java.vendor"));
    add("vm/version", System.getProperty("java.version"));
  }

  private void add(String key, String value) {
    information.put(key, new Precomputation(new SimpleStringish(value)));
  }

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public String getUriName() {
    return "current information";
  }

  @Override
  public Future resolveUri(TaskMaster task_master, String uri, Ptr<LibraryFailure> reason) {

    if (!uri.startsWith("current:")) return null;
    if (information.containsKey(uri.substring(8))) {
      reason.set(null);
      return information.get(uri.substring(8));
    }
    return null;
  }
}
