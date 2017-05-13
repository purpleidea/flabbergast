package flabbergast.lang;

interface ThunkeratorConsumer {
  void end();

  void next(SourceReference sourceReference, Context context, Thunkerator next);
}
