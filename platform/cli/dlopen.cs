using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;

namespace Flabbergast {
	public class BuiltInLibraries : UriHandler {
		public static readonly BuiltInLibraries INSTANCE = new BuiltInLibraries();
		public string UriName { get { return "built-in libraries"; } }
		private BuiltInLibraries() {}
		public Type ResolveUri(string uri, out bool stop) {
			stop = false;
			if (!uri.StartsWith("lib:"))
				return null;
			var type_name = "Flabbergast.Library." + uri.Substring(4).Replace('/', '.');
			return Type.GetType(type_name, false);
		}
	}
	public abstract class LoadLibraries : UriHandler {
		protected readonly List<string> paths = GenerateDefaultPaths();
		public static List<string> GenerateDefaultPaths() {
			var paths = new List<string>();
			var env_var = Environment.GetEnvironmentVariable("FLABBERGAST_PATH");
			if (env_var != null) {
			    paths.AddRange(env_var.Split(Path.PathSeparator).Select(Path.GetFullPath));
			}
			var full_path = Assembly.GetAssembly(typeof(Frame)).Location;
			var directory = Path.GetDirectoryName(full_path);
			paths.Add(Path.Combine(directory, "..", "lib", "flabbergast"));
			if (Environment.OSVersion.Platform == PlatformID.Unix || Environment.OSVersion.Platform == PlatformID.MacOSX) {
				paths.Add("/usr/lib/flabbergast");
				paths.Add("/usr/local/lib/flabbergast");
			}
			return paths;
		}
		public abstract string UriName { get; }
		public void AppendPath(string path) {
			paths.Add(path);
		}
		public void PrependPath(string path) {
			paths.Insert(0, path);
		}
		public abstract Type ResolveUri(string uri, out bool stop);
	}
	public class LoadPrecompiledLibraries : LoadLibraries  {
		public override string UriName { get { return "pre-compiled libraries"; } }
		public override Type ResolveUri(string uri, out bool stop) {
			stop = false;
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
					continue;
				}
				var type = assembly.GetType(type_name, false);
				if (type != null) {
					return type;
				}
			}
			return null;
		}
	}
}
