package flabbergast;

interface FlavourAttribute {
	public void generate(final Generator _generator, final LookupCache cache,
			final LoadableValue source_reference, final LoadableValue context,
			final LoadableValue self_frame,
			final LoadableValue container_frame,
			final LoadableValue _source_template, final LoadableValue _target,
			Generator.ParameterisedBlock<LoadableValue> _block)
			throws Exception;
}
