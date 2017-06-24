package flabbergast;

interface FlavourAccumulate {
  public void generate(
      Generator generator,
      LoadableValue source_reference,
      LoadableValue accumulator,
      Generator.ParameterisedBlock<LoadableValue> block)
      throws Exception;
}
