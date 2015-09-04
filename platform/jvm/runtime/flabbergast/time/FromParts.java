package flabbergast.time;

import flabbergast.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class FromParts extends BaseParts {
	private boolean first = true;
	private DateTimeZone zone;
	public FromParts(TaskMaster task_master, SourceReference source_ref,
			Context context, Frame self, Frame container) {
		super(task_master, source_ref, context, self, container);
	}
	@Override
	protected void run() {
		if (first) {
			first = false;
			interlock.set(2);
			new Lookup(task_master, source_reference, new String[]{"is_utc"},
					context).listen(new ConsumeResult() {
				@Override
				public void consume(Object is_utc) {
					if (is_utc instanceof Boolean) {
						zone = (Boolean) is_utc
								? DateTimeZone.UTC
								: DateTimeZone.getDefault();
						if (interlock.decrementAndGet() == 0) {
							task_master.slot(FromParts.this);
						}
					} else {
						task_master.reportOtherError(source_reference,
								"“is_utc” must be an Bool.");
					}
				}
			});
			lookupParts(false);
			if (interlock.decrementAndGet() > 0) {
				return;
			}
		}
		result = makeTime(new DateTime((int) years, (int) months, (int) days,
				(int) hours, (int) minutes, (int) seconds, (int) milliseconds,
				zone));
	}
}
