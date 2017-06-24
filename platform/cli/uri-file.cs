using System;
using System.IO;

namespace Flabbergast
{
    public class FileHandler : UriHandler
    {
        public static readonly FileHandler INSTANCE = new FileHandler();

        private FileHandler()
        {
        }

        public string UriName => "local files";

        public int Priority => 0;

        public Future ResolveUri(TaskMaster master, string uri, out LibraryFailure reason)
        {
            if (!uri.StartsWith("file:"))
            {
                reason = LibraryFailure.Missing;
                return null;
            }
            reason = LibraryFailure.None;
            try
            {
                return new Precomputation(File.ReadAllBytes(new Uri(uri).LocalPath));
            }
            catch (Exception e)
            {
                return new FailureFuture(master, new NativeSourceReference(uri), e.Message);
            }
        }
    }
}