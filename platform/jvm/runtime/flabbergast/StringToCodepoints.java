package flabbergast;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

public class StringToCodepoints extends Computation implements ConsumeResult {
	private AtomicInteger interlock = new AtomicInteger();
	private String input;

	private TaskMaster master;
	private SourceReference source_reference;
	private Context context;
	private Frame container;

	public StringToCodepoints(TaskMaster master, SourceReference source_ref,
			Context context, Frame self, Frame container) {
		this.master = master;
		this.source_reference = source_ref;
		this.context = context;
		this.container = self;
	}
	@Override
	public void consume(Object result) {
		if (result instanceof Stringish) {
			input = result.toString();
			if (interlock.decrementAndGet() == 0) {
				master.slot(this);
			}
		} else {
			master.reportOtherError(source_reference,
					"Input argument must be a string.");
		}
	}

	@Override
	protected void run() {
		if (input == null) {
			interlock.set(2);
			Computation input_lookup = new Lookup(master, source_reference,
					new String[]{"arg"}, context);
			input_lookup.listen(this);
			master.slot(input_lookup);
			if (interlock.decrementAndGet() > 0) {
				return;
			}
		}
		Frame frame = new Frame(master, master.nextId(), source_reference,
				context, container);
		for (int it = 0; it < input.length(); it++) {
			frame.set(TaskMaster.ordinalNameStr(it + 1),
					(long) input.codePointAt(it));
		}
		result = frame;
	}
}
