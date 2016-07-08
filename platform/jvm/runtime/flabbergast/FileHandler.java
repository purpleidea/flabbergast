package flabbergast;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


import flabbergast.TaskMaster.LibraryFailure;

public class FileHandler extends UrlConnectionHandler {

    public static final FileHandler INSTANCE = new FileHandler();

    public String getUriName() {
        return "local files";
    }
    @Override
    protected URL convert(String uri) throws Exception {

        if (!uri.startsWith("file:"))
            return null;
        return new URL(uri);
    }
}
