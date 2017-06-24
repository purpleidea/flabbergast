package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;
import java.net.URL;
import java.net.URLClassLoader;

public class LoadPrecompiledLibraries extends LoadLibraries {
  private URLClassLoader class_loader = null;

  public LoadPrecompiledLibraries() {}

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public String getUriName() {
    return "pre-compiled libraries";
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<? extends Future> resolveUri(String uri, Ptr<LibraryFailure> reason) {
    if (class_loader == null) {
      URL[] urls = new URL[getFinder().size()];
      for (int it = 0; it < urls.length; it++) {
        urls[it] = getFinder().get(it);
      }
      class_loader = new URLClassLoader(urls);
    }
    if (!uri.startsWith("lib:")) {
      return null;
    }

    String base_name = uri.substring(4).replace('/', '.');
    String type_name = "flabbergast.library." + base_name;
    try {
      return (Class<? extends Future>) class_loader.loadClass(type_name);
    } catch (ClassNotFoundException e) {
      reason.set(LibraryFailure.MISSING);
      return null;
    }
  }
}
