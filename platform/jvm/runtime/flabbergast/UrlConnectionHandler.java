package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public abstract class UrlConnectionHandler implements UriHandler {
  protected abstract URL convert(String uri) throws Exception;

  @Override
  public final Future resolveUri(TaskMaster task_master, String uri, Ptr<LibraryFailure> reason) {

    try {
      URL url = convert(uri);
      if (url == null) {
        reason.set(LibraryFailure.MISSING);
        return null;
      }
      URLConnection conn = new URL(uri).openConnection();
      byte[] data = new byte[conn.getContentLength()];
      InputStream inputStream = conn.getInputStream();
      for (int offset = 0;
          offset < data.length;
          offset += inputStream.read(data, offset, data.length - offset)) ;

      inputStream.close();
      return new Precomputation(data);
    } catch (Exception e) {
      return new FailureFuture(task_master, new NativeSourceReference(uri), e.getMessage());
    }
  }
}
