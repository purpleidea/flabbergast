package flabbergast;

interface FlavourAwareAccumulate {
  public void generate(
      Generator generator,
      LookupCache cache,
      LoadableValue source_reference,
      LoadableValue context,
      LoadableValue self_frame,
      LoadableValue container_frame,
      LoadableValue accumulator,
      Generator.ParameterisedBlock<LoadableValue> block)
      throws Exception;
}
