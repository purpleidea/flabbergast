using System;
using System.Collections.Generic;
using System.Globalization;

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

    protected void AddMap<T1, T2, T3, R>(string name, Func<T1, T2, T3, R> func, string parameter1, string parameter2) {
        Add(name,
            (task_master, source_ref, context, self, container) => new MapFunctionInterop<T1, T2, T3, R>(func, parameter1, parameter2, task_master, source_ref, context, self, container));
    }

    protected void AddMap<T1, T2, T3, T4, R>(string name, Func<T1, T2, T3, T4, R> func, string parameter1, string parameter2, string parameter3) {
        Add(name,
            (task_master, source_ref, context, self, container) => new MapFunctionInterop<T1, T2, T3, T4, R>(func, parameter1, parameter2, parameter3, task_master, source_ref, context, self, container));
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
        AddMap("math/circle/arccos", (double x, double angle_unit) => Math.Acos(x) / angle_unit, "angle_unit");
        AddMap("math/circle/arcsin", (double x, double angle_unit) => Math.Asin(x) / angle_unit, "angle_unit");
        AddMap("math/circle/arctan", (double x, double angle_unit) => Math.Atan(x) / angle_unit, "angle_unit");
        AddMap("math/circle/cos", (double x, double angle_unit) => Math.Cos(x * angle_unit), "angle_unit");
        AddMap("math/circle/sin", (double x, double angle_unit) => Math.Sin(x * angle_unit), "angle_unit");
        AddMap("math/circle/tan", (double x, double angle_unit) => Math.Tan(x * angle_unit), "angle_unit");
        AddMap<double, double>("math/floor", Math.Floor);
        AddMap("math/hyperbola/arccos", (double x, double angle_unit) => Math.Log(x + Math.Sqrt(x * x - 1.0)) / angle_unit, "angle_unit");
        AddMap("math/hyperbola/arcsin", (double x, double angle_unit) =>	Math.Log(x + Math.Sqrt(x * x + 1.0)) / angle_unit, "angle_unit");
        AddMap("math/hyperbola/arctan", (double x, double angle_unit) =>	0.5 * Math.Log((1.0 + x) / (1.0 - x)) / angle_unit, "angle_unit");
        AddMap("math/hyperbola/cos", (double x, double angle_unit) => Math.Cosh(x * angle_unit), "angle_unit");
        AddMap("math/hyperbola/sin", (double x, double angle_unit) => Math.Sinh(x * angle_unit), "angle_unit");
        AddMap("math/hyperbola/tan", (double x, double angle_unit) => Math.Tanh(x * angle_unit), "angle_unit");
        AddMap<double, double, double>("math/log", Math.Log, "real_base");
        AddMap<double, double, double>("math/power", Math.Pow, "real_exponent");
        AddMap<double, long, double>("math/round", (double x, long places) => Math.Round(x, (int) places), "real_places");
        Add("parse/json", (task_master, soure_ref, context, self, container) => new JsonParser(task_master, soure_ref, context, self, container));
        Add("sql/query", (task_master, soure_ref, context, self, container) => new DbQuery(task_master, soure_ref, context, self, container));
        AddMap<byte[], byte[]>("utils/bin/compress/gzip", BinaryFunctions.Compress);
        AddMap<string, byte[]>("utils/bin/from/base64",  Convert.FromBase64String);
        AddMap<byte[], byte[]>("utils/bin/hash/md5", BinaryFunctions.ComputeMD5);
        AddMap<byte[], byte[]>("utils/bin/hash/sha1", BinaryFunctions.ComputeSHA1);
        AddMap<byte[], byte[]>("utils/bin/hash/sha256", BinaryFunctions.ComputeSHA256);
        AddMap<byte[], string>("utils/bin/to/base64", Convert.ToBase64String);
        AddMap<byte[], string, bool, string>("utils/bin/to/hexstr", BinaryFunctions.BytesToHex, "delimiter", "uppercase");
        Add("utils/bin/uncompress/gzip", (task_master, soure_ref, context, self, container) => new Decompress(task_master, soure_ref, context, self, container));
        AddMap<double, bool, long, string>("utils/float/to/str", (x, exponential, digits) => x.ToString("0." + new string('0', (int)digits) + (exponential ? "E0" : "")) , "exponential", "digits");
        AddMap<long, bool, long, string>("utils/int/to/str", (x, hex, digits) => x.ToString((hex ? "X" : "D") +  digits) , "hex", "digits");
        AddMap<long, Stringish>("utils/ordinal", SupportFunctions.OrdinalName);
        Add("utils/parse/float", (task_master, soure_ref, context, self, container) => new ParseDouble(task_master, soure_ref, context, self, container));
        Add("utils/parse/int", (task_master, soure_ref, context, self, container) => new ParseInt(task_master, soure_ref, context, self, container));
        AddMap<string, bool, string>("utils/str/decode/punycode", (x, allow_unassigned) => {
            IdnMapping mapping = new IdnMapping();
            mapping.AllowUnassigned = allow_unassigned;
            return mapping.GetUnicode(x);
        }, "allow_unassigned");
        AddMap<string, bool, bool, string>("utils/str/encode/punycode", (x, allow_unassigned, strict_ascii) => {
            IdnMapping mapping = new IdnMapping();
            mapping.AllowUnassigned = allow_unassigned;
            mapping.UseStd3AsciiRules	= strict_ascii;
            return mapping.GetAscii(x);
        }, "allow_unassigned", "strict_ascii");
        Add("utils/str/escape", (task_master, soure_ref, context, self, container) => new Escape(task_master, soure_ref, context, self, container));
        AddMap < Stringish, string, long, bool, long?>("utils/str/find", (x, str, start, backward) => x.Find(str, start, backward), "str", "start", "backward");
        AddMap<long, string>("utils/str/from/codepoint", x => Char.ConvertFromUtf32((int) x));
        AddMap<byte[], string>("utils/str/from/utf16be", new System.Text.UnicodeEncoding(true, false, true).GetString);
        AddMap<byte[], string>("utils/str/from/utf16le", new System.Text.UnicodeEncoding(false, false, true).GetString);
        AddMap<byte[], string>("utils/str/from/utf32be",  new System.Text.UTF32Encoding(true, false, true).GetString);
        AddMap<byte[], string>("utils/str/from/utf32le", new System.Text.UTF32Encoding(false, false, true).GetString);
        AddMap<byte[], string>("utils/str/from/utf8", new System.Text.UTF8Encoding(false, true).GetString);
        AddMap<Stringish, bool>("utils/str/identifier", TaskMaster.VerifySymbol);
        AddMap<Stringish, long>("utils/str/length/utf16", x => x.Utf16Length);
        AddMap<Stringish, long>("utils/str/length/utf8", x => x.Utf8Length);
        AddMap<string, string>("utils/str/lower_case", x => x.ToLower());
        AddMap<string, string, bool>("utils/str/prefixed", (x, str) => x.StartsWith(str), "str");
        AddMap<string, string, string, string>("utils/str/replace", (x, str, with) => x.Replace(str, with), "str", "with");
        AddMap<Stringish, long, Nullable<long>, Nullable<long>, string>("utils/str/slice", (x, start, end, length) => x.Slice(start, end, length), "start", "end", "length");
        AddMap<string, string, bool>("utils/str/suffixed", (x, str) => x.EndsWith(str), "str");
        Add("utils/str/to/categories", (task_master, soure_ref, context, self, container) => new CharacterCategory(task_master, soure_ref, context, self, container));
        Add("utils/str/to/codepoints", (task_master, soure_ref, context, self, container) => new StringToCodepoints(task_master, soure_ref, context, self, container));
        AddMap<Stringish, byte[]>("utils/str/to/utf16be", x => x.ToUtf16(true));
        AddMap<Stringish, byte[]>("utils/str/to/utf16le", x => x.ToUtf16(false));
        AddMap<Stringish, byte[]>("utils/str/to/utf32be", x => x.ToUtf32(true));
        AddMap<Stringish, byte[]>("utils/str/to/utf32le", x => x.ToUtf32(false));
        AddMap<Stringish, byte[]>("utils/str/to/utf8", x => x.ToUtf8());
        AddMap<string, string>("utils/str/trim", x => x.Trim());
        AddMap<string, string>("utils/str/upper_case", x => x.ToUpper());
    }
}
}
