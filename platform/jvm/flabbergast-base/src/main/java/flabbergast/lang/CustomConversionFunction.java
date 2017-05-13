package flabbergast.lang;

import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;

class CustomConversionFunction<T> implements AnyFunction<ConversionOperation<? extends T>> {
  Function<? super byte[], ConversionOperation<? extends T>> binConversion =
      x -> ConversionOperation.fail(TypeExpectation.BIN);
  Function<? super Boolean, ConversionOperation<? extends T>> booleanConversion =
      x -> ConversionOperation.fail(TypeExpectation.BOOL);
  DoubleFunction<ConversionOperation<? extends T>> floatConversion =
      x -> ConversionOperation.fail(TypeExpectation.FLOAT);
  Function<? super Frame, ConversionOperation<? extends T>> frameConversion =
      x -> ConversionOperation.fail(TypeExpectation.FRAME);
  LongFunction<ConversionOperation<? extends T>> intConversion =
      x -> ConversionOperation.fail(TypeExpectation.INT);
  Function<? super LookupHandler, ConversionOperation<? extends T>> lookupHandlerConversion =
      x -> ConversionOperation.fail(TypeExpectation.LOOKUP_HANDLER);
  Supplier<ConversionOperation<? extends T>> nullConversion =
      () -> ConversionOperation.fail(TypeExpectation.NULL);
  Function<? super Str, ConversionOperation<? extends T>> strConversion =
      x -> ConversionOperation.fail(TypeExpectation.STR);
  Function<? super Template, ConversionOperation<? extends T>> templateConversion =
      x -> ConversionOperation.fail(TypeExpectation.TEMPLATE);

  CustomConversionFunction() {}

  @Override
  public ConversionOperation<? extends T> apply() {
    return nullConversion.get();
  }

  @Override
  public ConversionOperation<? extends T> apply(boolean value) {
    return booleanConversion.apply(value);
  }

  @Override
  public ConversionOperation<? extends T> apply(byte[] value) {
    return binConversion.apply(value);
  }

  @Override
  public ConversionOperation<? extends T> apply(double value) {
    return floatConversion.apply(value);
  }

  @Override
  public ConversionOperation<? extends T> apply(Frame value) {
    return frameConversion.apply(value);
  }

  @Override
  public ConversionOperation<? extends T> apply(long value) {
    return intConversion.apply(value);
  }

  @Override
  public ConversionOperation<? extends T> apply(LookupHandler value) {
    return lookupHandlerConversion.apply(value);
  }

  @Override
  public ConversionOperation<? extends T> apply(Str value) {
    return strConversion.apply(value);
  }

  @Override
  public ConversionOperation<? extends T> apply(Template value) {
    return templateConversion.apply(value);
  }
}
