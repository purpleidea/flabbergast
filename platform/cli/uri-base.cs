using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;

namespace Flabbergast {
public enum LibraryFailure {
    None,
    Missing,
    Corrupt,
    BadName
}

public interface UriHandler {
    string UriName {
        get;
    }
    int Priority {
        get;
    }
    Computation ResolveUri(TaskMaster task_master, string uri, out LibraryFailure reason);
}

public interface UriLoader {
    string UriName {
        get;
    }
    int Priority {
        get;
    }
    Type ResolveUri(string uri, out LibraryFailure reason);
}

public class UriInstaniator : UriHandler {
    private UriLoader loader;
    public UriInstaniator(UriLoader loader) {
        this.loader = loader;
    }
    public string UriName {
        get {
            return loader.UriName;
        }
    }
    public int Priority {
        get {
            return loader.Priority;
        }
    }
    public Computation ResolveUri(TaskMaster task_master, string uri, out LibraryFailure reason) {
        var type = loader.ResolveUri(uri, out reason);
        if (reason != LibraryFailure.None || type == null) {
            return null;
        }
        if (!typeof(Computation).IsAssignableFrom(type)) {
            throw new InvalidCastException(String.Format(
                                               "Class {0} for URI {1} from {2} is not a computation.", type, uri, UriName));
        }
        return (Computation) Activator.CreateInstance(type, task_master);
    }
}

[Flags]
public enum LoadRule {
    Sandboxed = 1,
    Interactive = 2,
    Precompiled = 4
}
public interface UriService {
    UriHandler Create(ResourcePathFinder finder, LoadRule rules);
}
}
