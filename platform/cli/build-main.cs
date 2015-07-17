using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;
using System.Text.RegularExpressions;
using NDesk.Options;

namespace Flabbergast {
	public class Builder {
		private static void Discover(string directory, List<string> sources, Dictionary<string, bool> known_dlls) {
			foreach (var path in Directory.EnumerateDirectories(directory)) {
				Discover(path, sources, known_dlls);
			}
			foreach (var file in Directory.EnumerateFiles(directory, "*.o_0")) {
				sources.Add(file);
			}
			foreach (var file in Directory.EnumerateFiles(directory, "*.no_0")) {
				sources.Add(file);
			}
			foreach (var file in Directory.EnumerateFiles(directory, "*.dll")) {
				known_dlls[file] = true;
			}
			foreach (var file in Directory.EnumerateFiles(directory, "*.dll.mdb")) {
				known_dlls[file] = true;
			}
		}
		public static int Main(string[] args) {
			if (args.Length != 0) {
				return 1;
			}

			var known_dlls = new Dictionary<string, bool>();
			var sources = new List<string>();
			Discover(".", sources, known_dlls);

			var collector = new ConsoleCollector();
			bool success = true;
			foreach (var filename in sources) {
				var parser = Parser.Open(Path.GetFullPath(filename));

				var dll_name = Path.ChangeExtension(Path.GetFileNameWithoutExtension(filename), ".dll");
				known_dlls.Remove(Path.Combine(".", dll_name));
				known_dlls.Remove(Path.Combine(".", Path.ChangeExtension(Path.GetFileNameWithoutExtension(filename), ".dll.mdb")));
				var type_name = Regex.Replace("Flabbergast.Library." + Path.GetDirectoryName(filename).Replace(Path.DirectorySeparatorChar, '.') + Path.GetFileNameWithoutExtension(filename), "\\.+", ".");
				var assembly_name = new AssemblyName(type_name) {CodeBase = "file://" + Path.GetDirectoryName(filename)};
				var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(assembly_name, AssemblyBuilderAccess.RunAndSave);
				CompilationUnit.MakeDebuggable(assembly_builder);
				var module_builder = assembly_builder.DefineDynamicModule(type_name, dll_name, true);
				var unit = new CompilationUnit(module_builder, true);
				var type = parser.ParseFile(collector, unit, type_name);
				if (type != null) {
					assembly_builder.Save(dll_name);
				} else {
					success = false;
				}
			}
			foreach (var dead in known_dlls.Keys) {
				File.Delete(dead);
			}
			return success ? 0 : 1;
		}
	}
}
