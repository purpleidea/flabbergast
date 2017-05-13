package flabbergast.lang;

interface WhinyAnyFunction<T> extends AnyFunction<ConversionOperation<? extends T>> {
  @Override
  default ConversionOperation<? extends T> apply() {
    return ConversionOperation.fail(TypeExpectation.NULL);
  }

  @Override
  default ConversionOperation<? extends T> apply(boolean value) {
    return ConversionOperation.fail(TypeExpectation.BOOL);
  }

  @Override
  default ConversionOperation<? extends T> apply(byte[] value) {
    return ConversionOperation.fail(TypeExpectation.BIN);
  }

  @Override
  default ConversionOperation<? extends T> apply(double value) {
    return ConversionOperation.fail(TypeExpectation.FLOAT);
  }

  @Override
  default ConversionOperation<? extends T> apply(Frame value) {
    return ConversionOperation.fail(TypeExpectation.FRAME);
  }

  @Override
  default ConversionOperation<? extends T> apply(long value) {
    return ConversionOperation.fail(TypeExpectation.INT);
  }

  @Override
  default ConversionOperation<? extends T> apply(LookupHandler value) {
    return ConversionOperation.fail(TypeExpectation.LOOKUP_HANDLER);
  }

  @Override
  default ConversionOperation<? extends T> apply(Str value) {
    return ConversionOperation.fail(TypeExpectation.STR);
  }

  @Override
  default ConversionOperation<? extends T> apply(Template value) {
    return ConversionOperation.fail(TypeExpectation.TEMPLATE);
  }
}
