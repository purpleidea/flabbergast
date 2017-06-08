using System;
using System.Collections.Generic;

namespace Flabbergast {
public abstract class Interop : UriHandler {
    private static readonly Frame NOTHING = new FixedFrame("interop", new NativeSourceReference("interop"));

    private Dictionary<String, Future> bindings = new Dictionary<String, Future>();

    public string UriName {
        get {
            return "library bindings";
        }
    }
    public int Priority {
        get {
            return 0;
        }
    }

    protected void Add(string name, ComputeValue compute) {
        if (bindings.ContainsKey(name)) {
            throw new InvalidOperationException("Duplicate interop: " + name);
        }
        var tmpl = new Template(NOTHING.SourceReference, null, NOTHING);
        tmpl["value"] = compute;
        bindings[name] = new Precomputation(tmpl);
    }

    protected void Add<T, R>(string name, Func<T, R> func, string parameter) {
        Add(name,
            (task_master, source_ref, context, self, container) => new FunctionInterop<T, R>(func, parameter, task_master, source_ref, context, self, container));
    }

    protected void Add<T1, T2 , R>(string name, Func<T1, T2, R> func, string parameter1, string parameter2) {
        Add(name,
            (task_master, source_ref, context, self, container) => new FunctionInterop<T1, T2, R>(func, parameter1, parameter2, task_master, source_ref, context, self, container));
    }

    protected void AddMap<T, R>(string name, Func<T, R> func) {
        Add(name,
            (task_master, source_ref, context, self, container) => new MapFunctionInterop<T, R>(func, task_master, source_ref, context, self, container));
    }

    protected void AddMap<T1, T2, R>(string name, Func<T1, T2, R> func, string parameter) {
        Add(name,
            (task_master, source_ref, context, self, container) => new MapFunctionInterop<T1, T2, R>(func, parameter, task_master, source_ref, context, self, container));
    }

    public Future ResolveUri(TaskMaster master, string uri, out LibraryFailure reason) {
        reason = LibraryFailure.Missing;
        if (!uri.StartsWith("interop:"))
            return null;
        if (bindings.ContainsKey(uri.Substring(8))) {
            reason = LibraryFailure.None;
            return bindings[uri.Substring(8)];
        }
        return null;
    }
}
public class StandardInterop : Interop {
    public static readonly StandardInterop INSTANCE = new StandardInterop();
    private StandardInterop() {
        AddMap<double, double>("math/abs", Math.Abs);
        AddMap<double, double>("math/ceiling", Math.Ceiling);
        AddMap<double, double>("math/floor", Math.Floor);
        AddMap<double, double, double>("math/log", Math.Log, "real_base");
        AddMap<double, double, double>("math/power", Math.Pow, "real_exponent");
        AddMap<double, long, double>("math/round", (double x, long places) => Math.Round(x, (int) places), "real_places");
        AddMap("math/circle/arccos", (double x, double angle_unit) => Math.Acos(x) / angle_unit, "angle_unit");
        AddMap("math/circle/arcsin", (double x, double angle_unit) => Math.Asin(x) / angle_unit, "angle_unit");
        AddMap("math/circle/arctan", (double x, double angle_unit) => Math.Atan(x) / angle_unit, "angle_unit");
        AddMap("math/circle/cos", (double x, double angle_unit) => Math.Cos(x * angle_unit), "angle_unit");
        AddMap("math/circle/sin", (double x, double angle_unit) => Math.Sin(x * angle_unit), "angle_unit");
        AddMap("math/circle/tan", (double x, double angle_unit) => Math.Tan(x * angle_unit), "angle_unit");
        AddMap("math/hyperbola/arccos", (double x, double angle_unit) => Math.Log(x + Math.Sqrt(x * x - 1.0)) / angle_unit, "angle_unit");
        AddMap("math/hyperbola/arcsin", (double x, double angle_unit) =>	Math.Log(x + Math.Sqrt(x * x + 1.0)) / angle_unit, "angle_unit");
        AddMap("math/hyperbola/arctan", (double x, double angle_unit) =>	0.5 * Math.Log((1.0 + x) / (1.0 - x)) / angle_unit, "angle_unit");
        AddMap("math/hyperbola/cos", (double x, double angle_unit) => Math.Cosh(x * angle_unit), "angle_unit");
        AddMap("math/hyperbola/sin", (double x, double angle_unit) => Math.Sinh(x * angle_unit), "angle_unit");
        AddMap("math/hyperbola/tan", (double x, double angle_unit) => Math.Tanh(x * angle_unit), "angle_unit");
    }
}
}
