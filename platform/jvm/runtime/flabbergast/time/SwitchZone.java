package flabbergast.time;

import flabbergast.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class SwitchZone extends BaseTime {
	private boolean first = true;
	private DateTime initial;
	private boolean to_utc;
	public SwitchZone(TaskMaster task_master, SourceReference source_ref,
			Context context, Frame self, Frame container) {
		super(task_master, source_ref, context, self, container);
	}
	@Override
	protected void run() {
		if (first) {
			first = false;
			interlock.set(3);
			getTime(new ConsumeDateTime() {
				@Override
				public void invoke(DateTime d) {
					initial = d;
				}
			}, "arg");
			new Lookup(task_master, source_reference, new String[]{"to_utc"},
					context).listen(new ConsumeResult() {
				@Override
				public void consume(Object to_utc) {
					if (to_utc instanceof Boolean) {
						SwitchZone.this.to_utc = (Boolean) to_utc;
						if (interlock.decrementAndGet() == 0) {
							task_master.slot(SwitchZone.this);
						}
					} else {
						task_master.reportOtherError(source_reference,
								"“to_utc” must be an Bool.");
					}
				}
			});
			if (interlock.decrementAndGet() > 0) {
				return;
			}
		}
		result = makeTime(initial.withZone(to_utc
				? DateTimeZone.UTC
				: DateTimeZone.getDefault()));
		return;
	}
}
