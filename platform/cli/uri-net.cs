using System;
using System.Net;

namespace Flabbergast
{
    public class FtpHandler : UriHandler
    {
        public static readonly FtpHandler INSTANCE = new FtpHandler();

        private FtpHandler()
        {
        }

        public string UriName => "FTP files";

        public int Priority => 0;

        public Future ResolveUri(TaskMaster master, string uri, out LibraryFailure reason)
        {
            if (!uri.StartsWith("ftp:") && !uri.StartsWith("ftps:"))
            {
                reason = LibraryFailure.Missing;
                return null;
            }
            reason = LibraryFailure.None;
            try
            {
                return new Precomputation(new WebClient().DownloadData(uri));
            }
            catch (Exception e)
            {
                return new FailureFuture(master, new NativeSourceReference(uri), e.Message);
            }
        }
    }

    public class HttpHandler : UriHandler
    {
        public static readonly HttpHandler INSTANCE = new HttpHandler();


        private HttpHandler()
        {
        }

        public string UriName => "HTTP files";

        public int Priority => 0;

        public Future ResolveUri(TaskMaster master, string uri, out LibraryFailure reason)
        {
            if (!uri.StartsWith("http:") && !uri.StartsWith("https:"))
            {
                reason = LibraryFailure.Missing;
                return null;
            }
            reason = LibraryFailure.None;
            var src_ref = new NativeSourceReference(uri);
            try
            {
                var response = WebRequest.Create(uri).GetResponse();
                var frame = new FixedFrame("http" + uri.GetHashCode(), src_ref);
                var data = new byte[response.ContentLength];
                var stream = response.GetResponseStream();
                for (var offset = 0; offset < data.Length; offset += stream.Read(data, offset, data.Length - offset)) ;

                frame.Add("data", data);
                frame.Add("content_type", response.ContentType);
                return new Precomputation(frame);
            }
            catch (Exception e)
            {
                return new FailureFuture(master, src_ref, e.Message);
            }
        }
    }
}