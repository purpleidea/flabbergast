using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;

namespace Flabbergast {
	public sealed class DynamicallyCompiledLibraries : LoadLibraries {
		public override string UriName { get { return "dynamically compiled libraries"; } }
		private readonly CompilationUnit unit;
		private readonly ErrorCollector collector;
		private readonly Dictionary<string, System.Type> cache = new Dictionary<string, System.Type>();

		public DynamicallyCompiledLibraries(ErrorCollector collector) {
			this.collector = collector;
			var name = "DynamicallyCompiledLibraries" + GetHashCode();
			var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName(name), AssemblyBuilderAccess.Run);
			CompilationUnit.MakeDebuggable(assembly_builder);
			var module_builder = assembly_builder.DefineDynamicModule(name, true);
			unit = new CompilationUnit(module_builder, true);
		}
		public override System.Type ResolveUri(string uri, out LibraryFailure reason) {
			if (cache.ContainsKey(uri)) {
				reason = cache[uri] == null ? LibraryFailure.Missing : LibraryFailure.None;
				return cache[uri];
			}
			var base_name = uri.Substring(4);
			var type_name = "Flabbergast.Library." + base_name.Replace('/', '.');
			foreach (var path in paths) {
				foreach (var extension in new[] { ".no_0", ".o_0"}) {
					var src_file = Path.Combine(path, base_name + extension);
					if (!File.Exists(src_file)) {
						continue;
					}
					var parser = Parser.Open(src_file);
					var type = parser.ParseFile(collector, unit, type_name);
					reason = type == null ? LibraryFailure.Corrupt : LibraryFailure.None;
					cache[uri] = type;
					return type;
				}
			}
			cache[uri] = null;
			reason = LibraryFailure.Missing;
			return null;
		}
	}
}

