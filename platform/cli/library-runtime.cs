using System;
using System.Collections.Generic;
namespace Flabbergast {

public class ConsoleTaskMaster : TaskMaster {
	public override void ReportLookupError(Lookup lookup, System.Type fail_type) {
		if (fail_type == null) {
			Console.Error.WriteLine("Undefined name “{0}”. Lookup was as follows:", lookup.Name);
		} else {
			Console.Error.WriteLine("Non-frame type {1} while resolving name “{0}”. Lookup was as follows:", lookup.Name, fail_type);
		}
		var col_width = Math.Max((int)Math.Log(lookup.FrameCount, 10) + 1, 3);
		for(var name_it = 0; name_it < lookup.NameCount; name_it++) {
			col_width = Math.Max(col_width, lookup.GetName(name_it).Length);
		}
		for(var name_it = 0; name_it < lookup.NameCount; name_it++) {
			Console.Error.Write("| {0}", lookup.GetName(name_it).PadRight(col_width, ' '));
		}
		Console.Error.WriteLine("|");
		var known_frames = new Dictionary<Frame, string>();
		var frame_list = new List<Frame>();
		var null_text = "| ".PadRight(col_width + 2, ' ');
		for(var frame_it = 0; frame_it < lookup.FrameCount; frame_it++) {
			for(var name_it = 0; name_it < lookup.NameCount; name_it++) {
				var frame = lookup[name_it, frame_it];
				if (frame == null) {
					Console.Error.Write(null_text);
					continue;
				}
				if (!known_frames.ContainsKey(frame)) {
					frame_list.Add(frame);
					known_frames[frame] = frame_list.Count.ToString().PadRight(col_width, ' ');
				}
				Console.Error.Write("| {0}", known_frames[frame]);
			}
			Console.Error.WriteLine("|");
		}
		for (var it = 0; it < frame_list.Count; it++) {
			Console.Error.WriteLine("Frame {0} defined:", it + 1);
			frame_list[it].SourceReference.Write(Console.Error, "  ");
		}
		Console.Error.WriteLine("Lookup happened here:");
		lookup.SourceReference.Write(Console.Error, "  ");
	}
	public override void ReportExternalError(string uri) {
		Console.Error.WriteLine("The URI “{0}” could not be resolved.", uri);
	}
	public override void ReportOtherError(SourceReference reference, string message) {
		Console.Error.WriteLine(message);
		reference.Write(Console.Error, "  ");
	}
}
public class PrintResult : Computation {
	private TaskMaster task_master;
	private string output_filename;
	private Computation source;
	public bool Success { get; private set; }
	public PrintResult(TaskMaster task_master, Computation source, string output_filename) {
		this.task_master = task_master;
		this.source = source;
		this.output_filename = output_filename;
	}

	protected override bool Run() {
		source.Notify(HandleFrameResult);
		task_master.Slot(source);
		return false;
	}

	private void HandleFrameResult(object result) {
		if (result is Frame) {
			var lookup = new Lookup(task_master, null, new string[] { "value" }, (result as Frame).Context);
			lookup.Notify(HandleFinalResult);
			task_master.Slot(lookup);
		} else {
			Console.Error.WriteLine("File did not contain a frame. That should be impossible.");
		}
	}
	private void HandleFinalResult(object result) {
		if (result is Stringish || result is long || result is bool || result is double) {
			Success = true;
			if (output_filename == null) {
				Console.WriteLine(result);
			} else {
				System.IO.File.WriteAllText(output_filename, result.ToString(), System.Text.Encoding.UTF8);
			}
		} else {
			Console.Error.WriteLine("Cowardly refusing to print result of type {0}.", result.GetType());
		}
	}
}
}
