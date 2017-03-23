using System;
using System.IO;
using System.Web;

namespace Flabbergast {
public class FileHandler : UriHandler {
    public static readonly FileHandler INSTANCE = new FileHandler();
    public string UriName {
        get {
            return "local files";
        }
    }
    public int Priority {
        get {
            return 0;
        }
    }

    private FileHandler() {
    }

    public Future ResolveUri(TaskMaster master, string uri, out LibraryFailure reason) {
        if (!uri.StartsWith("file:")) {
            reason = LibraryFailure.Missing;
            return null;
        }
        reason = LibraryFailure.None;
        try {
            return new Precomputation(File.ReadAllBytes(new Uri(uri).LocalPath));
        } catch (Exception e) {
            return new FailureFuture(master, new NativeSourceReference(uri), e.Message);
        }
    }
}
}
