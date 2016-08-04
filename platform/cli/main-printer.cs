using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;
using Flabbergast;
using NDesk.Options;

public class Printer {
    public static int Main(string[] args) {
        var trace = false;
        string output_filename = null;
        var show_help = false;
        var use_precompiled = true;
        var options = new OptionSet {
            {"o=|output", "Write output to file instead of standard output.", v => output_filename = v},
            {"t|trace-parsing", "Produce a trace of the parse process.", v => trace = v != null},
            {"p|no-precomp", "do not use precompiled libraries", v => use_precompiled = v == null},
            {"h|help", "show this message and exit", v => show_help = v != null}
        };

        List<string> files;
        try {
            files = options.Parse(args);
        } catch (OptionException e) {
            Console.Error.Write(AppDomain.CurrentDomain.FriendlyName + ": ");
            Console.Error.WriteLine(e.Message);
            Console.Error.WriteLine("Try “" + AppDomain.CurrentDomain.FriendlyName + " --help” for more information.");
            return 1;
        }

        if (show_help) {
            Console.WriteLine("Usage: " + AppDomain.CurrentDomain.FriendlyName + " input.o_0");
            Console.WriteLine("Run a Flabbergast file and display the “value” attribute.");
            Console.WriteLine();
            Console.WriteLine("Options:");
            options.WriteOptionDescriptions(Console.Out);
            return 1;
        }

        if (files.Count != 1) {
            Console.Error.WriteLine("Exactly one Flabbergast script must be given.");
            return 1;
        }

        var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName("Print"), AssemblyBuilderAccess.Run);
        CompilationUnit.MakeDebuggable(assembly_builder);
        var module_builder = assembly_builder.DefineDynamicModule("PrintModule", true);
        var unit = new CompilationUnit(module_builder, true);
        var collector = new ConsoleCollector();
        var task_master = new ConsoleTaskMaster();
        var resource_finder = new ResourcePathFinder();
        resource_finder.PrependPath(Path.Combine(Path.GetDirectoryName(Path.GetFullPath(files[0])), "lib"));
        resource_finder.AddDefault();
        task_master.AddUriHandler(new CurrentInformation(false));
        task_master.AddUriHandler(BuiltInLibraries.INSTANCE);
        var db_handler = new DbUriHandler();
        db_handler.Finder = resource_finder;
        task_master.AddUriHandler(db_handler);
        task_master.AddUriHandler(EnvironmentUriHandler.INSTANCE);
        task_master.AddUriHandler(HttpHandler.INSTANCE);
        task_master.AddUriHandler(FtpHandler.INSTANCE);
        task_master.AddUriHandler(FileHandler.INSTANCE);
        var resource_handler = new ResourceHandler();
        resource_handler.Finder = resource_finder;
        task_master.AddUriHandler(resource_handler);
        if (use_precompiled) {
            var precomp = new LoadPrecompiledLibraries();
            precomp.Finder = resource_finder;
            task_master.AddUriHandler(precomp);
        }
        var dyncomp = new DynamicallyCompiledLibraries(collector);
        dyncomp.Finder = resource_finder;
        task_master.AddUriHandler(dyncomp);
        var parser = Parser.Open(files[0]);
        parser.Trace = trace;
        var run_type = parser.ParseFile(collector, unit, "Printer");
        if (run_type != null) {
            var computation = (Computation) Activator.CreateInstance(run_type, task_master);
            var filewriter = new PrintResult(task_master, computation, output_filename);
            filewriter.Slot();
            task_master.Run();
            task_master.ReportCircularEvaluation();
            return filewriter.Success ? 0 : 1;
        }
        return 1;
    }
}
