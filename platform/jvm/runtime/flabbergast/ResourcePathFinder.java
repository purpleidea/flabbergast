package flabbergast;

import java.io.File;
import java.lang.Iterable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResourcePathFinder implements Iterable<String> {
    private final List<String> paths = new ArrayList<String>();

    public void addDefaults() {
        String env_var = System.getenv("FLABBERGAST_PATH");
        if (env_var != null) {
            for (String path : env_var.split(File.pathSeparator)) {
                if (path.trim().length() > 0) {
                    paths.add(new File(path).getAbsolutePath());
                }
            }
        }
        boolean isntWindows = !System.getProperty("os.name").startsWith(
                                  "Windows");
        if (isntWindows) {
            paths.add(System.getProperty("user.home") + File.separator
                      + ".local" + File.separator + "share" + File.separator
                      + "flabbergast" + File.separator + "lib");
        }
        try {
            String path = Frame.class.getProtectionDomain().getCodeSource()
                          .getLocation().toURI().getPath();
            paths.add(path + File.separator + ".." + File.separator + ".."
                      + File.separator + "flabbergast" + File.separator + "lib"
                      + File.separator + "flabbergast");
        } catch (URISyntaxException e) {
        }
        if (isntWindows) {
            paths.add("/usr/share/flabbergast/lib");
            paths.add("/usr/local/lib/flabbergast/lib");
        }
    }

    public void appendPath(String path) {
        if (path.trim().length() > 0) {
            paths.add(path);
        }
    }
    public List<File> findAll(String basename, String... extensions) {
        List<File> files = new ArrayList<File>();
        for (String path : paths) {
            for (String extension : extensions) {
                File file = new File(path, basename + extension);
                if (file.exists()) {
                    files.add(file);
                }
            }
        }
        return files;
    }
    public URL get(int index) {
        try {
            return new URL("file", "", paths.get(index));
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }
    @Override
    public Iterator<String> iterator() {
        return paths.iterator();
    }
    public void prependPath(String path) {
        if (path.trim().length() > 0) {
            paths.add(0, path);
        }
    }
    public int size() {
        return paths.size();
    }
}

