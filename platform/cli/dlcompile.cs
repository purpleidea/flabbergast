using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;

namespace Flabbergast {
	public class DynamicallyCompiledLibraries : LoadLibraries {
		public override string UriName { get { return "dynamically compiled libraries"; } }
		private CompilationUnit unit;
		private ErrorCollector collector;
		public DynamicallyCompiledLibraries(ErrorCollector collector) {
			this.collector = collector;
			var name = "DynamicallyCompiledLibraries" + GetHashCode();
			var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName(name), AssemblyBuilderAccess.Run);
			var module_builder = assembly_builder.DefineDynamicModule(name);
			unit = new CompilationUnit("<dynamic>", module_builder, false);
		}
		public override System.Type ResolveUri(string uri, out bool stop) {
			stop = false;
			var base_name = uri.Substring(4);
			var type_name = "Flabbergast.Library." + base_name.Replace('/', '.');
			foreach (var path in paths) {
				var src_file = Path.Combine(path, base_name + ".flbgst");
				if (!File.Exists(src_file)) {
					continue;
				}
				var parser = Parser.Open(src_file);
 				var type = parser.ParseFile(collector, unit, type_name);
				stop = type == null;
				return type;
			}
			return null;
		}
	}
}

