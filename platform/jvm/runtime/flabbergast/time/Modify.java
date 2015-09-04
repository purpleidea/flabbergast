package flabbergast.time;

import flabbergast.*;

import org.joda.time.DateTime;

public class Modify extends BaseParts {
	private boolean first = true;
	private DateTime initial;
	public Modify(TaskMaster task_master, SourceReference source_ref,
			Context context, Frame self, Frame container) {
		super(task_master, source_ref, context, self, container);
	}
	@Override
	protected void run() {
		if (first) {
			first = false;
			interlock.set(2);
			getTime(new ConsumeDateTime() {
				@Override
				public void invoke(DateTime d) {
					initial = d;
				}
			}, "arg");
			lookupParts(true);
			if (interlock.decrementAndGet() > 0) {
				return;
			}
		}
		result = makeTime(initial.plusMillis((int) milliseconds)
				.plusSeconds((int) seconds).plusMinutes((int) minutes)
				.plusHours((int) hours).plusDays((int) days)
				.plusMonths((int) months).plusYears((int) years));
	}
}
