package flabbergast.time;

import flabbergast.*;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

public class Compare extends BaseTime {
	private DateTime left;
	private DateTime right;
	private boolean first = true;

	public Compare(TaskMaster task_master, SourceReference source_ref,
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
				public void invoke(DateTime x) {
					left = x;
				}
			}, "arg");
			getTime(new ConsumeDateTime() {
				@Override
				public void invoke(DateTime x) {
					right = x;
				}
			}, "to");
			if (interlock.decrementAndGet() > 0) {
				return;
			}
		}
		result = (long) Seconds.secondsBetween(left, right).getSeconds();
	}
}
