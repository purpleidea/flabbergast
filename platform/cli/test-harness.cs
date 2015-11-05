using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Reflection.Emit;

namespace Flabbergast {
public class DirtyCollector : ErrorCollector {
    public bool AnalyseDirty {
        get;
        set;
    }
    public bool ParseDirty {
        get;
        set;
    }

    public void ReportExpressionTypeError(CodeRegion where, Type new_type, Type existing_type) {
        AnalyseDirty = true;
    }

    public void ReportLookupTypeError(CodeRegion environment, string name, Type new_type, Type existing_type) {
        AnalyseDirty = true;
    }

    public void ReportForbiddenNameAccess(CodeRegion environment, string name) {
        AnalyseDirty = true;
    }

    public void ReportRawError(CodeRegion where, string message) {
        AnalyseDirty = true;
    }

    public void ReportParseError(string filename, int index, int row, int column, string message) {
        ParseDirty = true;
    }

    public void ReportSingleTypeError(CodeRegion where, Type type) {
        AnalyseDirty = true;
    }
}

public class CheckResult : Computation {
    private readonly System.Type test_target;

    public CheckResult(TaskMaster task_master, System.Type test_target) : base(task_master) {
        this.test_target = test_target;
    }

    public bool Success {
        get;
        private set;
    }

    protected override bool Run() {
        var computation = (Computation) Activator.CreateInstance(test_target, task_master);
        computation.Notify(HandleFrameResult);
        return false;
    }

    private void HandleFrameResult(object result) {
        if (result is Frame) {
            var lookup = new Lookup(task_master, null, new[] {"value"}, ((Frame) result).Context);
            lookup.Notify(HandleFinalResult);
        }
    }

    private void HandleFinalResult(object result) {
        if (result is bool) {
            Success = (bool) result;
        }
    }
}

public class TestTaskMaster : TaskMaster {
    public override void ReportOtherError(SourceReference reference, string message) {
    }

    public override void ReportExternalError(string uri, LibraryFailure reason) {
    }
}

public class TestHarness {
    public static int Main(string[] args) {
        var uri = new Uri(Assembly.GetExecutingAssembly().GetName().CodeBase);
        var directory = Path.GetDirectoryName(uri.LocalPath);
        var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName("Test"),
                               AssemblyBuilderAccess.Run);
        var module_builder = assembly_builder.DefineDynamicModule("TestModule");
        var unit = new CompilationUnit(module_builder, false);
        var id = 0;
        var success = true;
        var lib = new DynamicallyCompiledLibraries(new DirtyCollector());
        lib.ClearPaths();
        lib.AppendPath(Path.Combine(directory, "..", "..", "..", "..", "lib"));
        success &= DoTests(Path.Combine(directory, "..", "..", "..", "..", "tests"), "*", unit, lib, ref id);
        success &= DoTests(Path.Combine(directory, "..", "..", "tests"), "I", unit, lib, ref id);
        return success ? 0 : 1;
    }

    public static List<string> GetFiles(string root, string child) {
        var files = new List<string>();
        var path = Path.Combine(root, child);
        if (Directory.Exists(path)) {
            files.AddRange(Directory.GetFiles(path).Where(file => file.EndsWith(".o_0")));
        }
        return files;
    }

    public static bool DoTests(string root, string type, CompilationUnit unit, UriLoader lib, ref int id) {
        var all_succeeded = true;
        if (!Directory.Exists(root)) {
            Console.WriteLine("Skipping non-existent directory: " + root);
            return all_succeeded;
        }
        foreach (var file in GetFiles(root, "malformed")) {
            var collector = new DirtyCollector();
            var parser = Parser.Open(file);
            parser.ParseFile(collector, unit, "Test" + id++);
            Console.WriteLine("{0} {1} {2} {3}", collector.ParseDirty ? "----" : "FAIL", "M", type,
                              Path.GetFileNameWithoutExtension(file));
            all_succeeded &= collector.ParseDirty;
        }
        var task_master = new TestTaskMaster();
        task_master.AddUriHandler(lib);
        foreach (var file in GetFiles(root, "errors")) {
            bool success;
            try {
                var collector = new DirtyCollector();
                var parser = Parser.Open(file);
                var test_type = parser.ParseFile(collector, unit, "Test" + id++);
                success = collector.AnalyseDirty;
                if (!success && test_type != null) {
                    var tester = new CheckResult(task_master, test_type);
                    tester.Slot();
                    task_master.Run();
                    success = !tester.Success;
                }
            } catch (Exception) {
                success = false;
            }
            Console.WriteLine("{0} {1} {2} {3}", success ? "----" : "FAIL", "E", type,
                              Path.GetFileNameWithoutExtension(file));
            all_succeeded &= success;
        }
        foreach (var file in GetFiles(root, "working")) {
            bool success;
            try {
                var collector = new DirtyCollector();
                var parser = Parser.Open(file);
                var test_type = parser.ParseFile(collector, unit, "Test" + id++);
                success = !collector.AnalyseDirty && !collector.ParseDirty;
                if (success && test_type != null) {
                    var tester = new CheckResult(task_master, test_type);
                    tester.Slot();
                    task_master.Run();
                    success = tester.Success;
                }
            } catch (Exception) {
                success = false;
            }
            Console.WriteLine("{0} {1} {2} {3}", success ? "----" : "FAIL", "W", type,
                              Path.GetFileNameWithoutExtension(file));
            all_succeeded &= success;
        }
        return all_succeeded;
    }
}
}
