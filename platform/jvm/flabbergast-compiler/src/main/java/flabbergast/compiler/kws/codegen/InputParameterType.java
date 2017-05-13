package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.KwsType.*;
import static flabbergast.compiler.kws.codegen.LanguageType.*;
import static flabbergast.compiler.kws.codegen.LanguageType.ATTRIBUTE_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.Streamable;
import flabbergast.compiler.kws.KwsType;
import flabbergast.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import org.objectweb.asm.Type;

abstract class InputParameterType {
  private static final Invokable ATTRIBUTE_SOURCE__OF =
      Invokable.staticInterfaceMethod(
          ATTRIBUTE_SOURCE_TYPE, ATTRIBUTE_SOURCE_TYPE, "of", ATTRIBUTE_ARRAY_TYPE);
  static final InputParameterType BUILDERS =
      new InputParameterType() {
        @Override
        void load(JvmBlock block, LoadableValue value) {
          throw new UnsupportedOperationException();
        }

        @Override
        void load(JvmBlock block, Streamable<LoadableValue> values) {
          final var attribueSourceArrayLocal =
              block.generator().newLocal(ATTRIBUTE_SOURCE_ARRAY_TYPE);
          final var sourceLocal = block.generator().newLocal(SOURCE_REFERENCE_TYPE);
          final var packedBuilders = new ArrayList<IntConsumer>();
          final var collector =
              new Consumer<LoadableValue>() {
                List<LoadableValue> currentAttributes = new ArrayList<>();

                @Override
                public void accept(LoadableValue loadableValue) {
                  if (loadableValue.type() == X) {
                    currentAttributes.add(loadableValue);
                  } else if (loadableValue.type() == R) {
                    pack();
                    packedBuilders.add(
                        index -> {
                          block.generator().loadLocal(attribueSourceArrayLocal);
                          block.generator().push(index);
                          loadableValue.load(block.generator());
                          block.generator().arrayStore(ATTRIBUTE_SOURCE_TYPE);
                        });
                  } else if (loadableValue.type() == T) {
                    pack();
                    packedBuilders.add(
                        index -> {
                          block.generator().loadLocal(attribueSourceArrayLocal);
                          block.generator().push(index);

                          loadableValue.load(block.generator());

                          block.loadLocation();
                          block.generator().loadLocal(sourceLocal);
                          TEMPLATE__JOIN_SOURCE_REFERENCE.invoke(block.generator());
                          block.generator().storeLocal(sourceLocal);

                          block.generator().arrayStore(ATTRIBUTE_SOURCE_TYPE);
                          block.generator().loadLocal(sourceLocal);
                        });
                  } else {
                    throw new UnsupportedOperationException();
                  }
                }

                private void pack() {
                  if (!currentAttributes.isEmpty()) {
                    final var looseBuilders = currentAttributes;
                    packedBuilders.add(
                        index -> {
                          block.generator().loadLocal(attribueSourceArrayLocal);
                          block.generator().push(index);
                          block.generator().push(looseBuilders.size());
                          block.generator().newArray(ATTRIBUTE_TYPE);
                          for (var i = 0; i < looseBuilders.size(); i++) {
                            block.generator().dup();
                            block.generator().push(i);
                            looseBuilders.get(i).load(block.generator());
                            block.generator().arrayStore(ATTRIBUTE_TYPE);
                          }
                          ATTRIBUTE_SOURCE__OF.invoke(block.generator());
                          block.generator().arrayStore(ATTRIBUTE_SOURCE_TYPE);
                        });
                    currentAttributes = new ArrayList<>();
                  }
                }
              };
          values.stream().forEachOrdered(collector);
          collector.pack();

          block.generator().storeLocal(sourceLocal);
          block.generator().push(packedBuilders.size());
          block.generator().newArray(ATTRIBUTE_SOURCE_TYPE);
          block.generator().storeLocal(attribueSourceArrayLocal);
          for (var i = 0; i < packedBuilders.size(); i++) {
            packedBuilders.get(i).accept(i);
          }
          block.generator().loadLocal(sourceLocal);
          block.generator().loadLocal(attribueSourceArrayLocal);
        }

        @Override
        void typeCheck(KwsType type, SourceLocation location, ErrorCollector collector) {
          collector.emitError(
              location, String.format("Parameter must be array of R|T|X but got single %s.", type));
        }

        @Override
        void typeCheck(Stream<KwsType> type, SourceLocation location, ErrorCollector collector) {
          type.forEach(
              t -> {
                if (t != R && t != T && t != X) {
                  collector.emitError(
                      location,
                      String.format(
                          "Parameter must be array of R|T|X but got array with element %s.", t));
                }
              });
        }
      };
  private static final Invokable NAME__OF_I =
      Invokable.staticMethod(NAME_TYPE, NAME_TYPE, "of", LONG_TYPE);
  private static final Invokable NAME__OF_S =
      Invokable.staticMethod(NAME_TYPE, NAME_TYPE, "of", STR_TYPE);
  static final InputParameterType NAMES =
      new InputParameterType() {
        @Override
        void load(JvmBlock block, LoadableValue value) {
          throw new UnsupportedOperationException();
        }

        @Override
        void load(JvmBlock block, Streamable<LoadableValue> values) {
          block.generator().push((int) values.stream().count());
          block.generator().newArray(NAME_TYPE);
          values
              .stream()
              .forEach(
                  new Consumer<>() {
                    private int index;

                    @Override
                    public void accept(LoadableValue loadableValue) {
                      block.generator().dup();
                      block.generator().push(index++);
                      loadableValue.load(block.generator());
                      if (loadableValue.type() == I) {
                        NAME__OF_I.invoke(block.generator());
                      } else if (loadableValue.type() == S) {
                        NAME__OF_S.invoke(block.generator());
                      } else {
                        throw new UnsupportedOperationException(
                            String.format("Cannot convert %s to Name.", loadableValue.type()));
                      }
                      block.generator().arrayStore(NAME_TYPE);
                    }
                  });
        }

        @Override
        void typeCheck(KwsType type, SourceLocation location, ErrorCollector collector) {
          collector.emitError(
              location, String.format("Parameter must be array of I|S but got single %s.", type));
        }

        @Override
        void typeCheck(Stream<KwsType> type, SourceLocation location, ErrorCollector collector) {
          type.forEach(
              t -> {
                if (t != I && t != S) {
                  collector.emitError(
                      location,
                      String.format(
                          "Parameter must be array of I|S but got array with element %s.", t));
                }
              });
        }
      };
  private static final Invokable TEMPLATE__JOIN_SOURCE_REFERENCE =
      Invokable.virtualMethod(
          TEMPLATE_TYPE,
          SOURCE_REFERENCE_TYPE,
          "joinSourceReference",
          JSTRING_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          SOURCE_REFERENCE_TYPE);

  static InputParameterType arrayOf(KwsType requiredType) {
    return new InputParameterType() {
      final Type elementType = LanguageType.of(requiredType);

      @Override
      void load(JvmBlock block, LoadableValue value) {
        throw new UnsupportedOperationException();
      }

      @Override
      void load(JvmBlock block, Streamable<LoadableValue> values) {
        block.generator().push((int) values.stream().count());
        block.generator().newArray(elementType);
        values
            .stream()
            .map(Pair.number())
            .forEachOrdered(
                b -> {
                  block.generator().dup();
                  block.generator().push(b.first());
                  b.second().load(block.generator());
                  block.generator().arrayStore(elementType);
                });
      }

      @Override
      void typeCheck(KwsType type, SourceLocation location, ErrorCollector collector) {
        collector.emitError(
            location,
            String.format("Parameter must be array of %s but got single %s.", requiredType, type));
      }

      @Override
      void typeCheck(Stream<KwsType> type, SourceLocation location, ErrorCollector collector) {
        type.forEach(
            t -> {
              if (t != requiredType) {
                collector.emitError(
                    location,
                    String.format(
                        "Parameter must be array of %s but got array with element %s.",
                        requiredType, t));
              }
            });
      }
    };
  }

  @SuppressWarnings("SameParameterValue")
  static InputParameterType fold(KwsType requiredType, Invokable folder) {
    return new InputParameterType() {

      @Override
      void load(JvmBlock block, LoadableValue value) {
        throw new UnsupportedOperationException();
      }

      @Override
      void load(JvmBlock block, Streamable<LoadableValue> values) {
        values
            .stream()
            .forEachOrdered(
                v -> {
                  v.load(block.generator());
                  folder.invoke(block.generator());
                });
      }

      @Override
      void typeCheck(KwsType type, SourceLocation location, ErrorCollector collector) {
        collector.emitError(
            location,
            String.format("Parameter must be array of %s but got single %s.", requiredType, type));
      }

      @Override
      void typeCheck(Stream<KwsType> type, SourceLocation location, ErrorCollector collector) {
        type.forEach(
            t -> {
              if (t != requiredType) {
                collector.emitError(
                    location,
                    String.format(
                        "Parameter must be array of %s but got array with element %s.",
                        requiredType, t));
              }
            });
      }
    };
  }

  static InputParameterType single(KwsType requiredType) {
    return new InputParameterType() {
      @Override
      void load(JvmBlock block, LoadableValue value) {
        value.load(block.generator());
      }

      @Override
      void load(JvmBlock block, Streamable<LoadableValue> values) {
        throw new UnsupportedOperationException();
      }

      @Override
      void typeCheck(KwsType type, SourceLocation location, ErrorCollector collector) {
        if (type != requiredType) {
          collector.emitError(
              location, String.format("Parameter must be %s but got %s.", requiredType, type));
        }
      }

      @Override
      void typeCheck(Stream<KwsType> type, SourceLocation location, ErrorCollector collector) {
        collector.emitError(
            location,
            String.format("Parameter must be %s but got array of %s.", requiredType, type));
      }
    };
  }

  abstract void load(JvmBlock block, LoadableValue value);

  abstract void load(JvmBlock block, Streamable<LoadableValue> values);

  abstract void typeCheck(KwsType type, SourceLocation location, ErrorCollector collector);

  abstract void typeCheck(Stream<KwsType> type, SourceLocation location, ErrorCollector collector);
}
