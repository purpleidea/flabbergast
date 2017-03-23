package flabbergast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.List;

import flabbergast.TaskMaster.LibraryFailure;

public class ResourceHandler implements UriHandler {

    private ResourcePathFinder finder;

    public String getUriName() {
        return "resource files";
    }
    public int getPriority() {
        return 0;
    }
    public ResourcePathFinder getFinder() {
        return finder;
    }

    public final Future resolveUri(TaskMaster task_master, String uri,
                                        Ptr<LibraryFailure> reason) {

        if (!uri.startsWith("res:")) {
            reason.set(LibraryFailure.MISSING);
            return null;
        }
        try {
            String tail = URLDecoder.decode(uri.substring(4), "UTF-8").replace('/', File.separatorChar);
            for (File file : getFinder().findAll(tail, "")) {
                byte[] data = new byte[(int)file.length()];
                InputStream inputStream = new FileInputStream(file);
                for (int offset = 0; offset < data.length; offset += inputStream.read(data, offset, data.length - offset)) ;

                inputStream.close();
                return new Precomputation(data);
            }
        } catch (Exception e) {
            return new FailureFuture(task_master, new NativeSourceReference(uri), e.getMessage());
        }
        reason.set(LibraryFailure.MISSING);
        return null;
    }
    public void setFinder(ResourcePathFinder finder) {
        this.finder = finder;
    }
}
