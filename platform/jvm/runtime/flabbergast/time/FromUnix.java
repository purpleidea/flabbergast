package flabbergast.time;

import flabbergast.*;

import org.joda.time.DateTime;

public class FromUnix extends BaseTime {
	public FromUnix(TaskMaster task_master, SourceReference source_ref,
			Context context, Frame self, Frame container) {
		super(task_master, source_ref, context, self, container);
	}
	@Override
	protected void run() {
		getUnixTime(new ConsumeDateTime() {
			@Override
			public void invoke(DateTime d) {
				result = makeTime(d);
				wakeupListeners();
			}
		}, context);
	}
}
