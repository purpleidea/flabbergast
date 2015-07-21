package flabbergast;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

public class StringToCodepoints extends Computation implements ConsumeResult {
	private AtomicInteger interlock = new AtomicInteger();
	private String input;

	private SourceReference source_reference;
	private Context context;
	private Frame container;

	public StringToCodepoints(TaskMaster task_master,
			SourceReference source_ref, Context context, Frame self,
			Frame container) {
		super(task_master);
		this.source_reference = source_ref;
		this.context = context;
		this.container = self;
	}
	@Override
	public void consume(Object result) {
		if (result instanceof Stringish) {
			input = result.toString();
			if (interlock.decrementAndGet() == 0) {
				task_master.slot(this);
			}
		} else {
			task_master.reportOtherError(source_reference,
					"Input argument must be a string.");
		}
	}

	@Override
	protected void run() {
		if (input == null) {
			interlock.set(2);
			Computation input_lookup = new Lookup(task_master,
					source_reference, new String[]{"arg"}, context);
			input_lookup.listen(this);
			if (interlock.decrementAndGet() > 0) {
				return;
			}
		}
		Frame frame = new Frame(task_master, task_master.nextId(),
				source_reference, context, container);
		for (int it = 0; it < input.length(); it++) {
			frame.set(TaskMaster.ordinalNameStr(it + 1),
					(long) input.codePointAt(it));
		}
		result = frame;
	}
}
