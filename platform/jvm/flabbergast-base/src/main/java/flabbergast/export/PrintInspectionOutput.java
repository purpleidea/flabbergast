package flabbergast.export;

import flabbergast.lang.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A pretty-printer of {@link Promise} for console output
 *
 * <p>This assumes the output device can be held open for the entire processing time
 */
public abstract class PrintInspectionOutput {
  private class PrintVisitor implements PromiseConsumer<Any> {
    private final String prefix;

    PrintVisitor(String prefix) {
      this.prefix = prefix;
    }

    @Override
    public void accept(Any value) {
      value.accept(
          new AnyConsumer() {

            @Override
            public void accept() {
              write("Null");
            }

            @Override
            public void accept(boolean result) {
              write(result ? "True" : "False");
            }

            @Override
            public void accept(byte[] result) {
              write(Integer.toString(result.length));
              write(" bytes of Unspeakable Horror");
            }

            @Override
            public void accept(double result) {
              write(Double.toString(result));
            }

            @Override
            public void accept(Frame f) {
              if (seen.containsKey(f)) {
                write(f.id().toString());
                write(" # Frame ");
                write(seen.get(f));
              } else {
                final var id = Integer.toString(seen.size());
                seen.put(f, id);

                final var isList = f.nameTypes().first() == 0;

                write(isList ? "[ # Frame " : "{ # Frame ");
                write(id);
                write("\n");
                final var visitor = new PrintVisitor(prefix + "  ");
                f.names()
                    .forEachOrdered(
                        name -> {
                          if (!isList) {
                            write(prefix);
                            write(name.toString());
                            write(" : ");
                          }
                          f.get(name).ifPresent(v -> v.accept(visitor));
                          write(isList ? ",\n" : "\n");
                        });
                write(prefix);
                write(isList ? "]" : "}");
              }
            }

            @Override
            public void accept(long result) {
              write(Long.toString(result));
            }

            @Override
            public void accept(LookupHandler value) {
              write("LookupHandler #");
              write(value.description());
            }

            @Override
            public void accept(Str result) {
              write("\"");
              final var s = result.toString();
              for (var it = 0; it < s.length(); it++) {
                if (s.charAt(it) == 7) {
                  write("\\a");
                } else if (s.charAt(it) == 8) {
                  write("\\b");
                } else if (s.charAt(it) == 12) {
                  write("\\f");
                } else if (s.charAt(it) == 10) {
                  write("\\n");
                } else if (s.charAt(it) == 13) {
                  write("\\r");
                } else if (s.charAt(it) == 9) {
                  write("\\t");
                } else if (s.charAt(it) == 11) {
                  write("\\v");
                } else if (s.charAt(it) == 34) {
                  write("\\\"");
                } else if (s.charAt(it) == 92) {
                  write("\\\\");
                } else if (s.charAt(it) < 16) {
                  write("\\x0");
                  write(Integer.toHexString(s.charAt(it)));
                } else if (s.charAt(it) < 32) {
                  write("\\x");
                  write(Integer.toHexString(s.charAt(it)));
                } else {
                  write(s.substring(it, it + 1));
                }
              }
              write("\"");
            }

            @Override
            public void accept(Template template) {
              write("Template {");
              template
                  .attributes()
                  .map(Attribute::name)
                  .forEachOrdered(
                      name -> {
                        write(" ");
                        write(name.toString());
                      });
              write(" }");
            }
          });
    }

    @Override
    public void unfinished() {
      write("<unfinished>");
    }
  }

  private final Map<Frame, String> seen = new HashMap<>();

  /** Create a new printer that has seen no frames */
  public PrintInspectionOutput() {}

  /** Write a promise to the output device */
  public final void print(Promise<Any> promise) {
    promise.accept(new PrintVisitor(""));
  }

  /** Write text to the output device */
  protected abstract void write(String string);
}
