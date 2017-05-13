package flabbergast.interop;

import flabbergast.lang.*;
import java.util.function.Consumer;

final class CustomLookupSelector implements LookupOperation<LookupSelector> {
  static void instantiate(
      Future<?> future,
      SourceReference sourceReference,
      Template currentTemplate,
      Any value,
      LookupNextOperation next,
      Consumer<Template> consumer) {
    Frame.create(
            future,
            sourceReference,
            Context.EMPTY,
            currentTemplate,
            AttributeSource.of(
                Attribute.of("empty", Any.of(false)), Attribute.of("current", value)))
        .get(Name.of("value"))
        .ifPresentOrElse(
            promise ->
                next.await(
                    promise,
                    any ->
                        any.accept(
                            LookupAction.CONVERTER.asConsumer(
                                future,
                                sourceReference,
                                TypeErrorLocation.UNKNOWN,
                                action ->
                                    action.perform(
                                        future, sourceReference, null, next, consumer)))),
            next::fail);
  }

  static void instantiateEmpty(
      Future<?> future,
      SourceReference sourceReference,
      Template currentTemplate,
      LookupLastOperation next) {
    Frame.create(
            future,
            sourceReference,
            Context.EMPTY,
            currentTemplate,
            AttributeSource.of(
                Attribute.of("empty", Any.of(true)), Attribute.of("current", Any.NULL)))
        .get(Name.of("value"))
        .ifPresentOrElse(
            promise ->
                next.await(
                    promise,
                    any ->
                        any.accept(
                            LookupAction.CONVERTER.asConsumer(
                                future,
                                sourceReference,
                                TypeErrorLocation.UNKNOWN,
                                action -> action.perform(future, next)))),
            next::fail);
  }

  private final Template selector;

  CustomLookupSelector(Template selector) {
    this.selector = selector;
  }

  @Override
  public String description() {
    return "custom";
  }

  @Override
  public LookupSelector start(Future<?> future, SourceReference sourceReference, Context context) {
    return new LookupSelector() {
      private Template currentTemplate = selector;

      @Override
      public void accept(Any value, LookupNextOperation next) {
        instantiate(
            future, sourceReference, currentTemplate, value, next, t -> currentTemplate = t);
      }

      @Override
      public void empty(LookupLastOperation next) {
        instantiateEmpty(future, sourceReference, currentTemplate, next);
      }
    };
  }
}
