using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;
using System.Text.RegularExpressions;
using NDesk.Options;

namespace Flabbergast {
public class Compiler {
    public static int Main(string[] args) {
        var trace = false;
        var show_help = false;
        var options = new OptionSet {
            {"t|trace-parsing", "Produce a trace of the parse process.", v => trace = v != null},
            {"h|help", "show this message and exit", v => show_help = v != null}
        };

        List<string> files;
        try {
            files = options.Parse(args);
        } catch (OptionException e) {
            Console.Write(AppDomain.CurrentDomain.FriendlyName + ": ");
            Console.WriteLine(e.Message);
            Console.WriteLine("Try “" + AppDomain.CurrentDomain.FriendlyName + " --help” for more information.");
            return 1;
        }

        if (show_help) {
            Console.WriteLine("Usage: " + AppDomain.CurrentDomain.FriendlyName + " files ...");
            Console.WriteLine("Compile a Flabbergast file to native CLR.");
            Console.WriteLine();
            Console.WriteLine("Options:");
            options.WriteOptionDescriptions(Console.Out);
            return 0;
        }
        if (files.Count == 0) {
            Console.WriteLine("Perhaps you wish to compile some source files?");
            return 1;
        }
        var collector = new ConsoleCollector();
        foreach (var filename in files) {
            var parser = Parser.Open(filename);
            parser.Trace = trace;

            var dll_name = Path.ChangeExtension(Path.GetFileNameWithoutExtension(filename), ".dll");
            var type_name = Regex.Replace("Flabbergast.Library." + Path.GetDirectoryName(filename).Replace(Path.DirectorySeparatorChar, '.') + Path.GetFileNameWithoutExtension(filename), "\\.+", ".");
            var assembly_name = new AssemblyName(type_name) {
                CodeBase = "file://" + Path.GetDirectoryName(filename)
            };
            var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(assembly_name, AssemblyBuilderAccess.RunAndSave);
            CompilationUnit.MakeDebuggable(assembly_builder);
            var module_builder = assembly_builder.DefineDynamicModule(type_name, dll_name, true);
            var unit = new CompilationUnit(module_builder, true);
            var type = parser.ParseFile(collector, unit, type_name);
            if (type != null) {
                assembly_builder.Save(dll_name);
            }
        }
        return 0;
    }
}
}
