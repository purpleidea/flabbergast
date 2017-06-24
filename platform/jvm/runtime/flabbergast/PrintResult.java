package flabbergast;

import java.io.FileWriter;
import java.io.IOException;

public class PrintResult extends Future {
  private final String output_filename;

  private final Future source;
  private boolean success;

  public PrintResult(TaskMaster task_master, Future source, String output_filename) {
    super(task_master);
    this.source = source;
    this.output_filename = output_filename;
  }

  public boolean getSuccess() {
    return success;
  }

  @Override
  protected void run() {
    source.listen(
        result -> {
          if (result instanceof Frame) {
            Frame frame = (Frame) result;
            Lookup lookup =
                new Lookup(
                    task_master,
                    new NativeSourceReference("printer"),
                    new String[] {"value"},
                    frame.getContext());
            lookup.listen(
                value -> {
                  if (value instanceof Stringish
                      || value instanceof Long
                      || value instanceof Boolean
                      || value instanceof Double) {
                    success = true;
                    if (output_filename == null) {
                      if (value instanceof Stringish) {
                        System.out.print(value);
                      } else if (value instanceof Boolean) {
                        System.out.println((Boolean) value ? "True" : "False");
                      } else {
                        System.out.println(value);
                      }
                    } else {
                      try {
                        FileWriter fw = new FileWriter(output_filename);
                        fw.write(value.toString());
                        if (!(value instanceof Stringish)) {
                          fw.write("\n");
                        }
                        fw.close();
                      } catch (IOException e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                      }
                    }
                  } else {
                    System.err.printf(
                        "Cowardly refusing to print result of type %s.\n",
                        SupportFunctions.nameForClass(value.getClass()));
                  }
                });
          } else {
            System.err.println("File did not contain a frame. That should be impossible.");
          }
        });
  }
}
