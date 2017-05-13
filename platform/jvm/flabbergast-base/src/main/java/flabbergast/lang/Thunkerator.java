package flabbergast.lang;

abstract class Thunkerator {
  abstract void iterator(Future<?> future, ThunkeratorConsumer thunkerator);
}
