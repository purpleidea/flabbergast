package flabbergast;

import java.io.FileWriter;
import java.io.IOException;

public class PrintResult extends Computation {
	private final String output_filename;

	private final Computation source;
	private boolean success;
	private final TaskMaster task_master;

	public PrintResult(TaskMaster task_master, Computation source,
			String output_filename) {
		this.task_master = task_master;
		this.source = source;
		this.output_filename = output_filename;
	}

	public boolean getSuccess() {
		return success;
	}

	@Override
	protected void run() {
		source.listen(new ConsumeResult() {

			@Override
			public void consume(Object result) {

				if (result instanceof Frame) {
					Frame frame = (Frame) result;
					Lookup lookup = new Lookup(task_master,
							new SourceReference("printer", "<native>", 0, 0, 0,
									0, null), new String[]{"value"}, frame
									.getContext());
					lookup.listen(new ConsumeResult() {

						@Override
						public void consume(Object result) {
							if (result instanceof Stringish
									|| result instanceof Long
									|| result instanceof Boolean
									|| result instanceof Double) {
								success = true;
								if (output_filename == null) {
									if (result instanceof Stringish) {
										System.out.print(result);
									} else if (result instanceof Boolean) {
										System.out.println((Boolean) result
												? "True"
												: "False");
									} else {
										System.out.println(result);
									}
								} else {
									try {
										FileWriter fw = new FileWriter(
												output_filename);
										fw.write(result.toString());
										if (!(result instanceof Stringish)) {
											fw.write("\n");
										}
										fw.close();
									} catch (IOException e) {
										System.err.println(e.getMessage());
										e.printStackTrace(System.err);
									}
								}
							} else {
								System.err
										.printf("Cowardly refusing to print result of type %s.\n",
												result.getClass());
							}

						}
					});
					task_master.slot(lookup);
				} else {
					System.err
							.println("File did not contain a frame. That should be impossible.");
				}

			}
		});
		task_master.slot(source);
	}
}
