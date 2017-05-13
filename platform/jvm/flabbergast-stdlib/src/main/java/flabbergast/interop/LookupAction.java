package flabbergast.interop;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import flabbergast.lang.Context.FrameAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

interface LookupAction {
  class FinishHolder implements LookupAssistant.Recipient {
    Any value;

    @Override
    public void run(Future<Any> future, SourceReference sourceReference, Context context) {
      future.complete(
          Any.of(
              Frame.proxyOf(sourceReference, context, LookupAction.finish(value), Stream.empty())));
    }
  }

  class ForkHolder implements LookupAssistant.Recipient {
    Map<Name, Any> value;

    @Override
    public void run(Future<Any> future, SourceReference sourceReference, Context context) {
      future.complete(
          Any.of(
              Frame.proxyOf(
                  sourceReference,
                  context,
                  LookupAction.fork(new ArrayList<>(value.values())),
                  Stream.empty())));
    }
  }

  AnyConverter<LookupAction> CONVERTER =
      AnyConverter.asProxy(
          LookupAction.class,
          false,
          SpecialLocation.library("lookup").attributes("actions").any().invoked());
  LookupAction FAIL =
      new LookupAction() {
        @Override
        public void perform(
            Future<?> future,
            SourceReference sourceReference,
            Context.FrameAccessor accessor,
            LookupNextOperation operation,
            Consumer<Template> nextTemplate) {
          operation.fail();
        }

        @Override
        public void perform(Future<?> future, LookupLastOperation operation) {
          operation.fail();
        }

        @Override
        public void performForked(
            Future<?> future,
            SourceReference sourceReference,
            FrameAccessor accessor,
            LookupForkOperation operation,
            Consumer<Template> nextTemplate) {
          perform(future, sourceReference, accessor, operation, nextTemplate);
        }
      };
  Definition FINISH =
      LookupAssistant.create(
          FinishHolder::new, LookupAssistant.find((i, v) -> i.value = v, "current"));
  Definition FORK =
      LookupAssistant.create(
          ForkHolder::new,
          LookupAssistant.find(AnyConverter.frameOfAny(false), (i, v) -> i.value = v, "args"));
  LookupAction NEXT =
      new LookupAction() {
        @Override
        public void perform(
            Future<?> future,
            SourceReference sourceReference,
            Context.FrameAccessor accessor,
            LookupNextOperation operation,
            Consumer<Template> nextTemplate) {
          operation.next();
        }

        @Override
        public void perform(Future<?> future, LookupLastOperation operation) {
          operation.fail();
        }

        @Override
        public void performForked(
            Future<?> future,
            SourceReference sourceReference,
            FrameAccessor accessor,
            LookupForkOperation operation,
            Consumer<Template> nextTemplate) {
          perform(future, sourceReference, accessor, operation, nextTemplate);
        }
      };

  static LookupAction access(Name name, Template continuation) {
    return new LookupAction() {
      @Override
      public void perform(
          Future<?> future,
          SourceReference sourceReference,
          Context.FrameAccessor accessor,
          LookupNextOperation operation,
          Consumer<Template> nextTemplate) {
        accessor
            .get(name)
            .ifPresentOrElse(
                promise ->
                    operation.await(
                        promise,
                        value ->
                            CustomLookupSelector.instantiate(
                                future, sourceReference, continuation, value, operation, null)),
                () ->
                    CustomLookupSelector.instantiateEmpty(
                        future, sourceReference, continuation, operation));
      }

      @Override
      public void perform(Future<?> future, LookupLastOperation operation) {
        operation.fail();
      }

      @Override
      public void performForked(
          Future<?> future,
          SourceReference sourceReference,
          FrameAccessor accessor,
          LookupForkOperation operation,
          Consumer<Template> nextTemplate) {
        perform(future, sourceReference, accessor, operation, nextTemplate);
      }
    };
  }

  static LookupAction finish(Any value) {
    return new LookupAction() {
      @Override
      public void perform(
          Future<?> future,
          SourceReference sourceReference,
          Context.FrameAccessor accessor,
          LookupNextOperation operation,
          Consumer<Template> nextTemplate) {
        operation.finish(value);
      }

      @Override
      public void perform(Future<?> future, LookupLastOperation operation) {
        operation.finish(value);
      }

      @Override
      public void performForked(
          Future<?> future,
          SourceReference sourceReference,
          FrameAccessor accessor,
          LookupForkOperation operation,
          Consumer<Template> nextTemplate) {
        perform(future, sourceReference, accessor, operation, nextTemplate);
      }
    };
  }

  static LookupAction fork(List<Any> items) {
    return new LookupAction() {

      @Override
      public void perform(
          Future<?> future,
          SourceReference sourceReference,
          FrameAccessor accessor,
          LookupNextOperation operation,
          Consumer<Template> nextTemplate) {
        operation.fail();
      }

      @Override
      public void perform(Future<?> future, LookupLastOperation operation) {
        operation.fail();
      }

      @Override
      public void performForked(
          Future<?> future,
          SourceReference sourceReference,
          FrameAccessor accessor,
          LookupForkOperation operation,
          Consumer<Template> nextTemplate) {
        operation.fork(items.stream());
      }
    };
  }

  static LookupAction then(Template template, LookupAction action) {
    return new LookupAction() {

      @Override
      public void perform(
          Future<?> future,
          SourceReference sourceReference,
          Context.FrameAccessor accessor,
          LookupNextOperation operation,
          Consumer<Template> nextTemplate) {
        if (nextTemplate == null) {
          operation.fail();
        } else {
          nextTemplate.accept(template);
          action.perform(future, sourceReference, accessor, operation, nextTemplate);
        }
      }

      @Override
      public void perform(Future<?> future, LookupLastOperation operation) {
        action.perform(future, operation);
      }

      @Override
      public void performForked(
          Future<?> future,
          SourceReference sourceReference,
          FrameAccessor accessor,
          LookupForkOperation operation,
          Consumer<Template> nextTemplate) {
        perform(future, sourceReference, accessor, operation, nextTemplate);
      }
    };
  }

  void perform(
      Future<?> future,
      SourceReference sourceReference,
      Context.FrameAccessor accessor,
      LookupNextOperation operation,
      Consumer<Template> nextTemplate);

  void perform(Future<?> future, LookupLastOperation operation);

  void performForked(
      Future<?> future,
      SourceReference sourceReference,
      Context.FrameAccessor accessor,
      LookupForkOperation operation,
      Consumer<Template> nextTemplate);
}
