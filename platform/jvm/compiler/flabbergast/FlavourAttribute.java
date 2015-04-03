package flabbergast;

interface FlavourAttribute {
	public void generate(final Generator _generator,
			final LoadableValue _source_template, final LoadableValue _target,
			Generator.ParameterisedBlock<LoadableValue> _block)
			throws Exception;
}
