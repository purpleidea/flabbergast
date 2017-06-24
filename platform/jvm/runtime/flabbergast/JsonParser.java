package flabbergast;

import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonParser extends BaseMapFunctionInterop<String, Template> {
  private static class Dispatch implements ComputeValue {
    Object name;
    Object node;

    public Dispatch(Object name, Object node) {
      this.name = name;
      this.node = node;
    }

    @Override
    public Future invoke(
        TaskMaster task_master,
        SourceReference src_ref,
        Context context,
        Frame self,
        Frame container) {
      Instantiation computation;
      if (node == null || node == JSONObject.NULL) {
        computation = new Instantiation(task_master, src_ref, context, self, "json", "scalar");
        computation.add("json_name", name);
        computation.add("arg", Unit.NULL);
      } else if (node instanceof JSONArray) {
        computation = new Instantiation(task_master, src_ref, context, self, "json", "list");
        computation.add("json_name", name);
        computation.add(
            "children",
            new ComputeValue() {

              @Override
              public Future invoke(
                  TaskMaster a_task_master,
                  SourceReference a_reference,
                  Context a_context,
                  Frame a_self,
                  Frame a_container) {
                MutableFrame a_arg_frame =
                    new MutableFrame(a_task_master, a_reference, a_context, a_self);
                JSONArray array = (JSONArray) node;
                for (int index = 0; index < array.length(); index++) {
                  try {
                    a_arg_frame.set(index + 1, new Dispatch(Unit.NULL, array.get(index)));
                  } catch (JSONException e) {
                    a_arg_frame.set(
                        index + 1, new FailureFuture(a_task_master, a_reference, e.getMessage()));
                  }
                }
                return new Precomputation(a_arg_frame);
              }
            });
      } else if (node instanceof Boolean || node instanceof Double || node instanceof Long) {
        computation = new Instantiation(task_master, src_ref, context, self, "json", "scalar");
        computation.add("json_name", name);
        computation.add("arg", node);
      } else if (node instanceof Integer) {
        int value = (Integer) node;
        computation = new Instantiation(task_master, src_ref, context, self, "json", "scalar");
        computation.add("json_name", name);
        computation.add("arg", (long) value);
      } else if (node instanceof JSONObject) {
        computation = new Instantiation(task_master, src_ref, context, self, "json", "object");
        computation.add("json_name", name);
        computation.add(
            "children",
            new ComputeValue() {
              @Override
              public Future invoke(
                  TaskMaster o_task_master,
                  SourceReference o_reference,
                  Context o_context,
                  Frame o_self,
                  Frame o_container) {
                MutableFrame o_arg_frame =
                    new MutableFrame(o_task_master, o_reference, o_context, o_self);
                int index = 1;
                JSONObject obj = (JSONObject) node;
                Iterator<String> it = obj.keys();
                while (it.hasNext()) {
                  String name = it.next();
                  try {
                    o_arg_frame.set(index, new Dispatch(new SimpleStringish(name), obj.get(name)));
                  } catch (JSONException e) {
                    o_arg_frame.set(
                        index, new FailureFuture(o_task_master, o_reference, e.getMessage()));
                  }
                  index++;
                }
                return new Precomputation(o_arg_frame);
              }
            });
      } else if (node instanceof String) {
        computation = new Instantiation(task_master, src_ref, context, self, "json", "scalar");
        computation.add("json_name", name);
        computation.add("arg", new SimpleStringish((String) node));
      } else {
        return new FailureFuture(task_master, src_ref, "Unknown JSON entry.");
      }
      return computation;
    }
  }

  public JsonParser(
      TaskMaster task_master,
      SourceReference source_reference,
      Context context,
      Frame self,
      Frame container) {
    super(Template.class, String.class, task_master, source_reference, context, self, container);
  }

  @Override
  protected Template computeResult(String input) throws Exception {
    JSONTokener json_value = new JSONTokener(input);
    Template tmpl = new Template(source_reference, context, self);
    tmpl.set("json_root", new Dispatch(Unit.NULL, json_value.nextValue()));
    return tmpl;
  }
}
