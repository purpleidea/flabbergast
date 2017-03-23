using System;
using System.Collections.Generic;

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
        information["login"] = new Precomputation(new SimpleStringish(Environment.UserName));
        information["directory"] = new Precomputation(new SimpleStringish(Environment.CurrentDirectory));
        information["version"] = new Precomputation(Configuration.Version);
        information["vm/name"] = new Precomputation(new SimpleStringish("CLR"));
        information["vm/vendor"] = new Precomputation(new SimpleStringish(Type.GetType("Mono.Runtime") != null ? "Mono" : "Microsoft"));
        information["vm/version"] = new Precomputation(new SimpleStringish(Environment.Version.ToString()));
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
