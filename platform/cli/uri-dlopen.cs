using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;

namespace Flabbergast {

public class BuiltInLibraries : UriLoader {
    public static readonly BuiltInLibraries INSTANCE = new BuiltInLibraries();
    private BuiltInLibraries() {}

    public string UriName {
        get {
            return "built-in libraries";
        }
    }
    public int Priority {
        get {
            return -100;
        }
    }

    public Type ResolveUri(string uri, out LibraryFailure reason) {
        reason = LibraryFailure.Missing;
        if (!uri.StartsWith("lib:"))
            return null;
        var type_name = "Flabbergast.Library." + uri.Substring(4).Replace('/', '.');
        var result = Type.GetType(type_name, false);
        if (result == null) {
            return null;
        } else {
            reason = LibraryFailure.None;
            return result;
        }
    }
}

public class ResourcePathFinder {
    protected readonly List<string> paths = new List<string>();
    public void AddDefault() {
        var env_var = Environment.GetEnvironmentVariable("FLABBERGAST_PATH");
        if (env_var != null) {
            paths.AddRange(env_var.Split(Path.PathSeparator).Where(x => !String.IsNullOrWhiteSpace(x)).Select(Path.GetFullPath));
        }
        paths.Add(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "flabbergast", "lib"));
        var full_path = Assembly.GetAssembly(typeof(Frame)).Location;
        var directory = Path.GetDirectoryName(full_path);
        paths.Add(Path.Combine(directory, "..", "lib"));
        if (Environment.OSVersion.Platform == PlatformID.Unix || Environment.OSVersion.Platform == PlatformID.MacOSX) {
            paths.Add("/usr/share/flabbergast/lib");
            paths.Add("/usr/local/share/flabbergast/lib");
        }
    }

    public void AppendPath(string path) {
        if (!String.IsNullOrWhiteSpace(path)) {
            paths.Add(path);
        }
    }

    public void ClearPaths() {
        paths.Clear();
    }

    public List<string> FindAll(string basename, params string[] extensions) {
        var found = new List<string>();
        foreach (var path in paths) {
            foreach (var extension in extensions) {
                var file = Path.Combine(path, basename + extension);
                if (File.Exists(file)) {
                    found.Add(file);
                }
            }
        }
        return found;
    }


    public void PrependPath(string path) {
        if (!String.IsNullOrWhiteSpace(path)) {
            paths.Insert(0, path);
        }
    }
}

public abstract class LoadLibraries : UriLoader {
    public ResourcePathFinder Finder {
        get;
        set;
    }
    public abstract string UriName {
        get;
    }
    public abstract int Priority {
        get;
    }
    public abstract Type ResolveUri(string uri, out LibraryFailure reason);
}

public class LoadPrecompiledLibraries : LoadLibraries {
    public override string UriName {
        get {
            return "pre-compiled libraries";
        }
    }
    public override int Priority {
        get {
            return 0;
        }
    }

    public override Type ResolveUri(string uri, out LibraryFailure reason) {
        var base_name = uri.Substring(4).Replace('/', '.');
        var type_name = "Flabbergast.Library." + base_name;
        foreach (var dll_file in Finder.FindAll(type_name, ".dll")) {
            var assembly = Assembly.LoadFrom(dll_file);
            if (assembly == null) {
                reason = LibraryFailure.Corrupt;
                return null;
            }
            var type = assembly.GetType(type_name, false);
            if (type == null) {
                reason = LibraryFailure.Corrupt;
                return null;
            }
            reason = LibraryFailure.None;
            return type;
        }
        reason = LibraryFailure.Missing;
        return null;
    }
}
}
