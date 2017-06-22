using System;
using System.Json;
using System.Threading;

namespace Flabbergast {
public class JsonParser : BaseMapFunctionInterop<string, Template> {
    public JsonParser(TaskMaster task_master, SourceReference source_reference,
                      Context context, Frame self, Frame container) : base(task_master, source_reference, context, self, container) {
    }

    internal static ComputeValue Dispatch(object name, JsonValue node) {
        return (TaskMaster task_master, SourceReference source_reference, Context context, Frame self, Frame container) => {
            if (node == null) {
                return new Instantiation(task_master, source_reference, context, self, "json", "scalar") {
                    { "json_name", name }, { "arg", Unit.NULL }
                };
            }
            switch (node.JsonType) {
            case JsonType.Array:
                return new Instantiation(task_master, source_reference, context, self, "json", "list") {
                    { "json_name", name }, { "children",
                        (ComputeValue)((TaskMaster a_task_master, SourceReference a_reference, Context a_context, Frame a_self, Frame a_container) => {
                            var a_arg_frame = new MutableFrame(a_task_master, a_reference, a_context, a_self);
                            var index = 1;
                            foreach (var item in(JsonArray) node) {
                                a_arg_frame.Set(index, Dispatch(Unit.NULL, item));
                                index++;
                            }
                            return new Precomputation(a_arg_frame);
                        })
                    }
                };
            case JsonType.Boolean:
                return new Instantiation(task_master, source_reference, context, self, "json", "scalar") {
                    { "json_name", name }, { "arg", (bool) node }
                };
            case JsonType.Number:
                return new Instantiation(task_master, source_reference, context, self, "json", "scalar") {
                    { "json_name", name }, { "arg", (double) node }
                };
            case JsonType.Object:
                return new Instantiation(task_master, source_reference, context, self, "json", "object") {
                    { "json_name", name }, { "children",
                        (ComputeValue)((TaskMaster o_task_master, SourceReference o_reference, Context o_context, Frame o_self, Frame o_container) => {
                            var o_arg_frame = new MutableFrame(o_task_master, o_reference, o_context, o_self);
                            var index = 1;
                            foreach (var entry in(JsonObject) node) {
                                o_arg_frame.Set(index, Dispatch(new SimpleStringish(entry.Key), entry.Value));
                                index++;
                            }
                            return new Precomputation(o_arg_frame);
                        })
                    }
                };
            case JsonType.String:
                return new Instantiation(task_master, source_reference, context, self, "json", "scalar") {
                    { "json_name", name }, { "arg", new SimpleStringish((string) node) }
                };
            default:
                return new FailureFuture(task_master, source_reference, "Unknown JSON entry.");
            }
        };
    }
    protected override Template ComputeResult(string input) {
        var json_value = JsonValue.Parse(input);
        var tmpl = new Template(source_reference,  context, self);
        tmpl["json_root"] = Dispatch(Unit.NULL, json_value);
        return tmpl;
    }
}
}
