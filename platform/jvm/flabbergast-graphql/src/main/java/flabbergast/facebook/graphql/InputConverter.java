package flabbergast.facebook.graphql;

import static flabbergast.facebook.graphql.PerformQuery.TO_STR;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import flabbergast.lang.*;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

final class InputConverter implements Function<Frame, ConversionOperation<? extends Input>> {

  private static final AnyConverter<Input> LIST_INPUT_CONVERTER =
      AnyConverter.of(
          AnyConverter.convertBool(InputConverter::apply),
          AnyConverter.convertFloat(InputConverter::apply),
          AnyConverter.convertInt(InputConverter::apply),
          AnyConverter.convertStr(InputConverter::apply),
          AnyConverter.convertFrame(new InputConverter(true)));
  static final AnyConverter<Input> OBJECT_INPUT_CONVERTER =
      AnyConverter.of(
          AnyConverter.convertBool(InputConverter::apply),
          AnyConverter.convertFloat(InputConverter::apply),
          AnyConverter.convertInt(InputConverter::apply),
          AnyConverter.convertStr(InputConverter::apply),
          AnyConverter.convertFrame(new InputConverter(false)));

  static ConversionOperation<? extends Input> apply(boolean value) {
    return ConversionOperation.succeed(
        new Input() {

          @Override
          public void write(ObjectNode writer, String name) {
            writer.put(name, value);
          }

          @Override
          public void write(ArrayNode writer) {
            writer.add(value);
          }
        });
  }

  static ConversionOperation<? extends Input> apply(double value) {
    return ConversionOperation.succeed(
        new Input() {

          @Override
          public void write(ObjectNode writer, String name) {
            writer.put(name, value);
          }

          @Override
          public void write(ArrayNode writer) {
            writer.add(value);
          }
        });
  }

  static ConversionOperation<? extends Input> apply(long value) {
    return ConversionOperation.succeed(
        new Input() {

          @Override
          public void write(ObjectNode writer, String name) {
            writer.put(name, value);
          }

          @Override
          public void write(ArrayNode writer) {
            writer.add(value);
          }
        });
  }

  static ConversionOperation<? extends Input> apply(Str value) {
    return ConversionOperation.succeed(
        new Input() {

          @Override
          public void write(ObjectNode writer, String name) {
            writer.put(name, value.toString());
          }

          @Override
          public void write(ArrayNode writer) {
            writer.add(value.toString());
          }
        });
  }

  private final boolean isInList;

  InputConverter(boolean isInList) {
    this.isInList = isInList;
  }

  @Override
  public ConversionOperation<? extends Input> apply(Frame frame) {
    return ConversionOperation.extractProxy(
        frame,
        Input.class,
        value -> {
          final var distribution = value.nameTypes();
          if (distribution.second() == 0) {
            return ConversionOperation.frame(value, OBJECT_INPUT_CONVERTER)
                .map(
                    items ->
                        new Input() {

                          @Override
                          public void write(ObjectNode writer, String name) {
                            final var node = writer.putObject(name);
                            for (final var f : items.entrySet()) {
                              f.getValue().write(node, f.getKey().apply(TO_STR));
                            }
                          }

                          @Override
                          public void write(ArrayNode writer) {
                            final var node = writer.addObject();
                            for (final var f : items.entrySet()) {
                              f.getValue().write(node, f.getKey().apply(TO_STR));
                            }
                          }
                        });
          }

          if (distribution.first() == 0) {
            if (isInList) {
              return ConversionOperation.<Input>fail(
                  "A list may not contain other lists directly in GraphQL.");
            }
            return ConversionOperation.frame(value, LIST_INPUT_CONVERTER)
                .map(
                    items ->
                        new Input() {

                          @Override
                          public void write(ObjectNode writer, String name) {
                            final var node = writer.putArray(name);

                            items
                                .entrySet()
                                .stream()
                                .sorted(Comparator.comparing(Map.Entry::getKey))
                                .map(Map.Entry::getValue)
                                .forEach(item -> item.write(node));
                          }

                          @Override
                          public void write(ArrayNode writer) {
                            throw new UnsupportedOperationException(
                                "Lists cannot contain lists in GraphQL inputs");
                          }
                        });
          }
          return ConversionOperation.<Input>fail("Frame containing numeric and string attributes");
        });
  }
}
