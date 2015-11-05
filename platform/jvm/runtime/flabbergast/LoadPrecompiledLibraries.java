package flabbergast;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import flabbergast.TaskMaster.LibraryFailure;

public class LoadPrecompiledLibraries extends LoadLibraries {
    private URLClassLoader class_loader = null;

    public LoadPrecompiledLibraries() {
    }

    @Override
    public String getUriName() {
        return "pre-compiled libraries";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Computation> resolveUri(String uri,
            Ptr<LibraryFailure> reason) {
        if (class_loader == null) {
            URL[] urls = new URL[paths.size()];
            for (int it = 0; it < urls.length; it++) {
                try {
                    urls[it] = new URL("file", "", paths.get(it));
                } catch (MalformedURLException e) {
                    System.err.println(e.getMessage());
                }

            }
            class_loader = new URLClassLoader(urls);
        }
        if (!uri.startsWith("lib:")) {
            return null;
        }

        String base_name = uri.substring(4).replace('/', '.');
        String type_name = "flabbergast.library." + base_name;
        try {
            return (Class<? extends Computation>) class_loader
                   .loadClass(type_name);
        } catch (ClassNotFoundException e) {
            reason.set(LibraryFailure.MISSING);
            return null;
        }
    }
}