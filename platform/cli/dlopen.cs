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
	public class BuiltInLibraries : UriLoader {
		public static readonly BuiltInLibraries INSTANCE = new BuiltInLibraries();
		private BuiltInLibraries() {}

		public string UriName {
			get { return "built-in libraries"; }
		}

		public Type ResolveUri(string uri, out LibraryFailure reason) {
			reason = LibraryFailure.None;
			if (!uri.StartsWith("lib:"))
				return null;
			var type_name = "Flabbergast.Library." + uri.Substring(4).Replace('/', '.');
			var result = Type.GetType(type_name, false);
			if (result == null) {
				reason = LibraryFailure.Missing;
				return null;
			} else {
				return result;
			}
		}
	}

	public abstract class LoadLibraries : UriLoader {
		protected readonly List<string> paths = GenerateDefaultPaths();
		public abstract string UriName { get; }
		public abstract Type ResolveUri(string uri, out LibraryFailure reason);

		public void AppendPath(string path) {
			paths.Add(path);
		}

		public static List<string> GenerateDefaultPaths() {
			var paths = new List<string>();
			var env_var = Environment.GetEnvironmentVariable("FLABBERGAST_PATH");
			if (env_var != null) {
				paths.AddRange(env_var.Split(Path.PathSeparator).Select(Path.GetFullPath));
			}
			paths.Add(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "flabbergast", "lib"));
			var full_path = Assembly.GetAssembly(typeof(Frame)).Location;
			var directory = Path.GetDirectoryName(full_path);
			paths.Add(Path.Combine(directory, "..", "lib"));
			if (Environment.OSVersion.Platform == PlatformID.Unix || Environment.OSVersion.Platform == PlatformID.MacOSX) {
				paths.Add("/usr/share/flabbergast/lib");
				paths.Add("/usr/local/share/flabbergast/lib");
			}
			return paths;
		}

		public void PrependPath(string path) {
			paths.Insert(0, path);
		}
	}

	public class LoadPrecompiledLibraries : LoadLibraries {
		public override string UriName {
			get { return "pre-compiled libraries"; }
		}

		public override Type ResolveUri(string uri, out LibraryFailure reason) {
			var base_name = uri.Substring(4).Replace('/', '.');
			var type_name = "Flabbergast.Library." + base_name;
			var dll_name = base_name + ".dll";
			foreach (var path in paths) {
				var dll_file = Path.Combine(path, dll_name);
				if (!File.Exists(dll_file)) {
					continue;
				}
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
