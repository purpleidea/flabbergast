package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class HttpHandler implements UriHandler {

  public static final HttpHandler INSTANCE = new HttpHandler();

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public String getUriName() {
    return "HTTP files";
  }

  @Override
  public final Future resolveUri(TaskMaster task_master, String uri, Ptr<LibraryFailure> reason) {

    if (!uri.startsWith("http:") && !uri.startsWith("https:")) {
      reason.set(LibraryFailure.MISSING);
      return null;
    }
    try {
      URL url = new URL(uri);
      if (url == null) {
        reason.set(LibraryFailure.MISSING);
      }
      URLConnection conn = new URL(uri).openConnection();
      byte[] data = new byte[conn.getContentLength()];
      InputStream inputStream = conn.getInputStream();
      for (int offset = 0;
          offset < data.length;
          offset += inputStream.read(data, offset, data.length - offset)) ;

      inputStream.close();
      FixedFrame frame = new FixedFrame("http" + uri.hashCode(), new NativeSourceReference(uri));
      frame.add("data", data);
      frame.add("content_type", conn.getContentType());
      return new Precomputation(frame);
    } catch (Exception e) {
      return new FailureFuture(task_master, new NativeSourceReference(uri), e.getMessage());
    }
  }
}
