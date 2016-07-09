using System;
using System.Collections.Generic;
using System.IO;
using System.Web;

namespace Flabbergast {
public class ResourceHandler : UriHandler {
    public string UriName {
        get {
            return "resource files";
        }
    }

    private readonly List<string> paths = LoadLibraries.GenerateDefaultPaths();

    public void AppendPath(string path) {
        paths.Add(path);
    }

    public void ClearPaths() {
        paths.Clear();
    }

    public void PrependPath(string path) {
        paths.Insert(0, path);
    }
    public Computation ResolveUri(TaskMaster master, string uri, out LibraryFailure reason) {
        if (!uri.StartsWith("res:")) {
            reason = LibraryFailure.Missing;
            return null;
        }
        var tail = HttpUtility.UrlDecode(uri.Substring(4)).Replace('/', Path.DirectorySeparatorChar);
        try {
            foreach (var path in paths) {
                var filename = Path.Combine(path, tail);
                if (File.Exists(filename)) {
                    reason = LibraryFailure.None;
                    return new Precomputation(File.ReadAllBytes(filename));

                }
            }
            reason = LibraryFailure.Missing;
            return null;
        } catch (Exception e) {
            reason = LibraryFailure.None;
            return new FailureComputation(master, new NativeSourceReference(uri), e.Message);
        }
    }
}
}
