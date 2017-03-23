using System;
using System.Collections.Generic;
using System.IO;
using System.Web;

namespace Flabbergast {
public class ResourceHandler : UriHandler {
    public ResourcePathFinder Finder {
        get;
        set;
    }
    public string UriName {
        get {
            return "resource files";
        }
    }
    public int Priority {
        get {
            return 0;
        }
    }

    public Future ResolveUri(TaskMaster master, string uri, out LibraryFailure reason) {
        if (!uri.StartsWith("res:")) {
            reason = LibraryFailure.Missing;
            return null;
        }
        var tail = HttpUtility.UrlDecode(uri.Substring(4)).Replace('/', Path.DirectorySeparatorChar);
        try {
            foreach (var filename in Finder.FindAll(tail, "")) {
                reason = LibraryFailure.None;
                return new Precomputation(File.ReadAllBytes(filename));
            }
            reason = LibraryFailure.Missing;
            return null;
        } catch (Exception e) {
            reason = LibraryFailure.None;
            return new FailureFuture(master, new NativeSourceReference(uri), e.Message);
        }
    }
}
}
