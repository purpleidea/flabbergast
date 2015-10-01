using System;
using System.IO;
using System.Json;
using System.Net;
namespace Flabbergast {
public class JsonUriHandler : UriHandler {
    public static readonly JsonUriHandler INSTANCE = new JsonUriHandler();
    internal static ComputeValue Dispatch(object name, JsonValue node) {
        return (TaskMaster task_master, SourceReference src_ref, Context context, Frame self, Frame container) => {
            if (node == null) {
                return new Instantiation(task_master, src_ref, context, self, "json", "scalar") { { "json_name", name }, { "arg", Unit.NULL }
                };
            }
            switch (node.JsonType) {
            case JsonType.Array:
                return new Instantiation(task_master, src_ref, context, self, "json", "list") { { "json_name", name }, { "children",
                    (ComputeValue) ((TaskMaster a_task_master, SourceReference a_reference, Context a_context, Frame a_self, Frame a_container) => {
                        var a_arg_frame = new MutableFrame(a_task_master, a_reference, a_context, a_self);
                        var index = 1;
                        foreach (var item in (JsonArray) node) {
                            a_arg_frame.Set(index, Dispatch(Unit.NULL, item));
                            index++;
                        }
                        return new Precomputation(a_arg_frame);
                    })
                                                                                                                           }
                };
            case JsonType.Boolean:
                return new Instantiation(task_master, src_ref, context, self, "json", "scalar") { { "json_name", name }, { "arg", (bool) node }
                };
            case JsonType.Number:
                return new Instantiation(task_master, src_ref, context, self, "json", "scalar") { { "json_name", name }, { "arg", (double) node }
                };
            case JsonType.Object:
                return new Instantiation(task_master, src_ref, context, self, "json", "object") { { "json_name", name }, { "children",
                    (ComputeValue) ((TaskMaster o_task_master, SourceReference o_reference, Context o_context, Frame o_self, Frame o_container) => {
                        var o_arg_frame = new MutableFrame(o_task_master, o_reference, o_context, o_self);
                        var index = 1;
                        foreach (var entry in (JsonObject) node) {
                            o_arg_frame.Set(index, Dispatch(new SimpleStringish(entry.Key), entry.Value));
                            index++;
                        }
                        return new Precomputation(o_arg_frame);
                    })
                                                                                                                             }
                };
            case JsonType.String:
                return new Instantiation(task_master, src_ref, context, self, "json", "scalar") { { "json_name", name }, { "arg", new SimpleStringish((string) node) }
                };
            default:
                return new FailureComputation(task_master, src_ref, "Unknown JSON entry.");
            }
        };
    }
    private JsonUriHandler() {}
    public string UriName {
        get {
            return "JSON importer";
        }
    }
    public Computation ResolveUri(TaskMaster task_master, string uri, out LibraryFailure reason) {
        if (!uri.StartsWith("json:")) {
            reason = LibraryFailure.Missing;
            return null;
        }
        reason = LibraryFailure.None;
        try {
            string json_text;
            var uri_obj = new Uri(uri.Substring(5));
            if (uri_obj.Scheme == "file") {
                json_text = File.ReadAllText(uri_obj.LocalPath);
            } else {
                json_text = new WebClient().DownloadString(uri_obj);
            }
            var json_value = JsonValue.Parse(json_text);
            var tmpl = new Template(new NativeSourceReference(uri), null, null);
            tmpl["json_root"] = Dispatch(Unit.NULL, json_value);
            return new Precomputation(tmpl);
        } catch (Exception e) {
            return new FailureComputation(task_master, new NativeSourceReference(uri), e.InnerException == null ? e.Message : e.InnerException.Message);
        }
    }
}
}
