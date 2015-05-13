package flabbergast;

interface FlavourRepl {
	public void generate(Generator generator, LoadableValue root_frame,
			LoadableValue current_frame, LoadableValue update_current,
			LoadableValue escape_value, LoadableValue print_value,
			Generator.ParameterisedBlock<LoadableValue> block) throws Exception;
}
