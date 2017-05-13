package flabbergast.lang;

import flabbergast.lang.Fricassee.FetchDefinition;
import java.util.List;
import java.util.function.Consumer;

/**
 * Create a “zipping” source that combines the attribute values of several frames
 *
 * <p>This is the usual <tt>For x : y, n : Name</tt>.
 */
public abstract class FricasseeZipper {

  /** Add frame for a particular attribute name. */
  public static FricasseeZipper frame(String name, Frame frame) {
    return frame(name, Context.EMPTY.accessor(frame));
  }
  /** Add frame for a particular attribute name with potentially private access. */
  public static FricasseeZipper frame(String name, Frame frame, Context context) {
    return frame(name, context.accessor(frame));
  }
  /** Add frame for a particular attribute name with potentially private access. */
  public static FricasseeZipper frame(Str name, Frame frame, Context context) {
    return frame(name.toString(), frame, context);
  }
  /** Add frame for a particular attribute name with potentially private access. */
  public static FricasseeZipper frame(String name, Context.FrameAccessor frame) {
    return new FricasseeZipper() {

      @Override
      void prepare(List<FetchDefinition> sources, Consumer<Frame> frameForNames) {
        frameForNames.accept(frame.frame());
        sources.add(
            (future, sourceReference, ordinal, attribute, output) ->
                frame
                    .get(attribute)
                    .ifPresentOrElse(
                        promise ->
                            future.await(
                                promise,
                                sourceReference,
                                String.format("Attribute “%s” in fricassée zipper", name),
                                value -> output.accept(Attribute.of(name, value))),
                        () -> output.accept(Attribute.of(name, Any.NULL))));
      }
    };
  }

  /** Add frame for a particular attribute name. */
  public static FricasseeZipper frame(Str name, Frame frame) {
    return frame(name.toString(), frame);
  }

  /** Add a <tt>Name</tt> definition. */
  public static FricasseeZipper name(String name) {
    return new FricasseeZipper() {

      @Override
      protected void prepare(List<FetchDefinition> sources, Consumer<Frame> frameForNames) {
        sources.add(
            (future, sourceReference, ordinal, attribute, output) ->
                output.accept(Attribute.of(name, attribute.any())));
      }
    };
  }

  /** Add a <tt>Name</tt> definition. */
  public static FricasseeZipper name(Str name) {
    return name(name.toString());
  }

  /** Add an <tt>Ordinal</tt> definition. */
  public static FricasseeZipper ordinal(String name) {
    return new FricasseeZipper() {

      @Override
      protected void prepare(List<FetchDefinition> sources, Consumer<Frame> frameForNames) {
        sources.add(
            (future, sourceReference, ordinal, attribute, output) ->
                output.accept(Attribute.of(name, Any.of(ordinal))));
      }
    };
  }

  /** Add an <tt>Ordinal</tt> definition. */
  public static FricasseeZipper ordinal(Str name) {
    return ordinal(name.toString());
  }

  private FricasseeZipper() {}

  abstract void prepare(List<FetchDefinition> sources, Consumer<Frame> frameForNames);
}
