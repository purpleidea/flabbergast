using System;
using System.Collections.Generic;
using System.IO;

namespace Flabbergast {
public class CurrentInformation : UriHandler {
    private Dictionary<String, Future> information = new Dictionary<String, Future>();

    public string UriName {
        get {
            return "current information";
        }
    }
    public int Priority {
        get {
            return 0;
        }
    }

    public CurrentInformation(bool interactive) {
        information["interactive"] = new Precomputation(interactive);
        Add("login", Environment.UserName);
        Add("directory", Environment.CurrentDirectory);
        information["version"] = new Precomputation(Configuration.Version);
        Add("machine/directory_separator", Path.DirectorySeparatorChar.ToString());
        Add("machine/name", Environment.OSVersion.VersionString);
        Add("machine/path_separator", Path.PathSeparator.ToString());
        Add("machine/line_ending", Environment.NewLine);
        Add("vm/name", "CLR");
        Add("vm/vendor", Type.GetType("Mono.Runtime") != null ? "Mono" : "Microsoft");
        Add("vm/version", Environment.Version.ToString());
    }

    private void Add(string key, string value) {
        information[key] = new Precomputation(new SimpleStringish(value));
    }

    public Future ResolveUri(TaskMaster master, string uri, out LibraryFailure reason) {
        reason = LibraryFailure.Missing;
        if (!uri.StartsWith("current:"))
            return null;
        if (information.ContainsKey(uri.Substring(8))) {
            reason = LibraryFailure.None;
            return information[uri.Substring(8)];
        }
        return null;
    }
}
}
