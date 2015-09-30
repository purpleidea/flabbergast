package flabbergast;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class Instantiation extends Computation {
	private String[] names;
	private Map<String, Object> overrides = new HashMap<String, Object>();
	private Context context;
	private Frame container;
	private Template tmpl;
	private SourceReference src_ref;
	private AtomicInteger interlock = new AtomicInteger(2);
	public Instantiation(TaskMaster task_master, SourceReference src_ref,
			Context context, Frame container, String... names) {
		super(task_master);
		this.src_ref = src_ref;
		this.context = context;
		this.container = container;
		this.names = names;
	}

	public void add(String name, Object val) {
		overrides.put(name, val);
	}

	@Override
	protected void run() {
		if (tmpl == null) {
			new Lookup(task_master, src_ref, names, context)
					.listen(new ConsumeResult() {
						@Override
						public void consume(Object tmpl_result) {
							if (tmpl_result instanceof Template) {
								tmpl = (Template) tmpl_result;
								if (interlock.decrementAndGet() == 0) {
									task_master.slot(Instantiation.this);
								}
							} else {
								StringBuilder name = new StringBuilder();
								for (int i = 0; i < names.length; i++) {
									if (i > 0) {
										name.append(".");
									}
									name.append(names[i]);
								}
								task_master.reportOtherError(
										src_ref,
										String.format(
												"Expected “%s” to be a Template but got %s.",
												name,
												Stringish
														.nameForClass(tmpl_result
																.getClass())));
							}
						}
					});
			if (interlock.decrementAndGet() > 0) {
				return;
			}
		}
		MutableFrame frame = new MutableFrame(task_master,
				new JunctionReference("instantiation", "<native>", 0, 0, 0, 0,
						src_ref, tmpl.getSourceReference()), Context.append(
						context, tmpl.getContext()), container);
		for (Entry<String, Object> entry : overrides.entrySet()) {
			frame.set(entry.getKey(), entry.getValue());
		}
		for (String name : tmpl) {
			if (!overrides.containsKey(name)) {
				frame.set(name, tmpl.get(name));
			}
		}
		result = frame;
		return;
	}
}
