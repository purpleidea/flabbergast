using System;
using System.Collections.Generic;
namespace Flabbergast {
public interface CodeRegion {
	int StartRow { get; }
	int StartColumn { get; }
	int EndRow { get; }
	int EndColumn { get; }
	string FileName { get; }
}
public interface ErrorCollector {
	void ReportExpressionTypeError(CodeRegion where, Type new_type, Type existing_type);
	void ReportLookupTypeError(CodeRegion where, string name, Type new_type, Type existing_type);
	void ReportForbiddenNameAccess(CodeRegion where, string name);
	void ReportParseError(string filename, int index, int row, int column, string message);
	void ReportRawError(CodeRegion where, string message);
}
public class ConsoleCollector : ErrorCollector {
	public void ReportExpressionTypeError(CodeRegion where, Type new_type, Type existing_type) {
		Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: Expression has conflicting types: {5} versus {6}.", where.FileName, where.StartRow, where.StartColumn, where.EndRow, where.EndColumn, new_type, existing_type);
	}
	public void ReportLookupTypeError(CodeRegion environment, string name, Type new_type, Type existing_type) {
		Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: Lookup for “{5}” has conflicting types: {6} versus {7}.", environment.FileName, environment.StartRow, environment.StartColumn, environment.EndRow, environment.EndColumn, name, new_type, existing_type);
	}
	public void ReportForbiddenNameAccess(CodeRegion environment, string name) {
		Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: Lookup for “{5}” is forbidden.", environment.FileName, environment.StartRow, environment.StartColumn, environment.EndRow, environment.EndColumn, name);
	}
	public void ReportParseError(string filename, int index, int row, int column, string message) {
		Console.Error.WriteLine("{0}:{1}:{2}: {3}", filename, row, column, message);
	}
	public void ReportRawError(CodeRegion where, string message) {
		Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: {5}", where.FileName, where.StartRow, where.StartColumn, where.EndRow, where.EndColumn, message);
	}
}

public class ConsoleTaskMaster : TaskMaster {
	public override void ReportLookupError(Lookup lookup) {
		Console.Error.WriteLine("Undefined name “{0}”. Lookup was as follows:", lookup.Name);
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
		var frame_id = 1;
		var null_text = "| ".PadRight(col_width, ' ');
		for(var frame_it = 0; frame_it < lookup.FrameCount; frame_it++) {
			for(var name_it = 0; name_it < lookup.NameCount; name_it++) {
				var frame = lookup[name_it, frame_it];
				if (frame == null) {
					Console.Error.Write(null_text);
					continue;
				}
				if (!known_frames.ContainsKey(frame)) {
					known_frames[frame] = (frame_id++).ToString().PadRight(col_width, ' ');
					frame_list.Add(frame);
				}
				Console.Error.Write("| {0}", known_frames[frame]);
			}
			Console.Error.WriteLine("|");
		}
		for (var it = 0; it < frame_list.Count; it++) {
			Console.Error.WriteLine("Frame {0} defined:", it + 1);
			frame_list[it].SourceReference.Write(Console.Error, 0, "  ");
		}
		Console.Error.WriteLine("Lookup happened here:");
		lookup.SourceReference.Write(Console.Error, 0, "  ");
	}
	public override void ReportOtherError(SourceReference reference, string message) {
		Console.Error.WriteLine(message);
		reference.Write(Console.Error, 0, "  ");
	}
}
}
