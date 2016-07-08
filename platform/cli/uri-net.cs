using System;
using System.IO;
using System.Net;

namespace Flabbergast {
public class FtpHandler : UriHandler {
    public static readonly FtpHandler INSTANCE = new FtpHandler();
    public string UriName {
        get {
            return "FTP files";
        }
    }

    private FtpHandler() {
    }

    public Computation ResolveUri(TaskMaster master, string uri, out LibraryFailure reason) {
        if (!uri.StartsWith("ftp:") && !uri.StartsWith("ftps:")) {
            reason = LibraryFailure.Missing;
            return null;
        }
        reason = LibraryFailure.None;
        try {
            return new Precomputation(new WebClient().DownloadData(uri));
        } catch (Exception e) {
            return new FailureComputation(master, new NativeSourceReference(uri), e.Message);

        }
    }
}

public class HttpHandler : UriHandler {
    public static readonly HttpHandler INSTANCE = new HttpHandler();
    public string UriName {
        get {
            return "HTTP files";
        }
    }

    private HttpHandler() {
    }

    public Computation ResolveUri(TaskMaster master, string uri, out LibraryFailure reason) {
        if (!uri.StartsWith("http:") && !uri.StartsWith("https:")) {
            reason = LibraryFailure.Missing;
            return null;
        }
        reason = LibraryFailure.None;
        var src_ref = new NativeSourceReference(uri);
        try {
            var response = WebRequest.Create(uri).GetResponse();
            var frame = new FixedFrame("http" + uri.GetHashCode(), src_ref);
            var data = new byte[response.ContentLength];
            var stream = response.GetResponseStream();
            for (var offset = 0; offset < data.Length; offset += stream.Read(data, offset, data.Length - offset));

            frame.Add("data", data);
            frame.Add("content_type", response.ContentType);
            return new Precomputation(frame);
        } catch (Exception e) {
            return new FailureComputation(master, src_ref, e.Message);

        }
    }

}
}
