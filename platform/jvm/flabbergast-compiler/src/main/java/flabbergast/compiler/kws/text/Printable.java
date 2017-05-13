package flabbergast.compiler.kws.text;

import flabbergast.compiler.Streamable;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface Printable {
  static Printable all(Streamable<Printable> items) {
    return sb -> {
      print(sb.append("("), items.stream());
      sb.append(")");
    };
  }

  static Printable block(TextBlock block, Streamable<Printable> arguments) {
    return sb -> {
      sb.append(block.id());
      sb.append("(");
      Printable.print(sb, arguments.stream());
      sb.append(")");
    };
  }

  static Printable of(String s) {
    return sb -> sb.append(s);
  }

  static void print(StringBuilder sb, Stream<Printable> items) {
    items.forEach(
        new Consumer<>() {
          boolean first = true;

          public void accept(Printable item) {
            if (!first) {
              sb.append(", ");
            }
            item.appendTo(sb);
            first = false;
          }
        });
  }

  void appendTo(StringBuilder sb);
}
