using System;

namespace Flabbergast {
	public interface CodeRegion {
		int EndColumn { get; }
		int EndRow { get; }
		string FileName { get; }
		string PrettyName { get; }
		int StartColumn { get; }
		int StartRow { get; }
	}

	public interface ErrorCollector {
		void ReportExpressionTypeError(CodeRegion where, Type new_type, Type existing_type);
		void ReportForbiddenNameAccess(CodeRegion where, string name);
		void ReportLookupTypeError(CodeRegion where, string name, Type new_type, Type existing_type);
		void ReportParseError(string filename, int index, int row, int column, string message);
		void ReportRawError(CodeRegion where, string message);
		void ReportSingleTypeError(CodeRegion where, Type type);
	}

	public class ConsoleCollector : ErrorCollector {
		public void ReportExpressionTypeError(CodeRegion where, Type new_type, Type existing_type) {
			if (existing_type == 0) {
				Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: No possible type for {5}. Expression should have types: {6}.", where.FileName, where.StartRow, where.StartColumn, where.EndRow, where.EndColumn, where.PrettyName, new_type);
			} else {
				Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: Conflicting types for {5}: {6} versus {7}.", where.FileName, where.StartRow, where.StartColumn, where.EndRow, where.EndColumn, where.PrettyName, new_type, existing_type);
			}
		}

		public void ReportLookupTypeError(CodeRegion environment, string name, Type new_type, Type existing_type) {
			if (existing_type == 0) {
				Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: No possible type for “{5}”. Expression should have types: {6}.", environment.FileName, environment.StartRow, environment.StartColumn, environment.EndRow, environment.EndColumn, name, new_type);
			} else {
				Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: Lookup for “{5}” has conflicting types: {6} versus {7}.", environment.FileName, environment.StartRow, environment.StartColumn, environment.EndRow, environment.EndColumn, name, new_type, existing_type);
			}
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

		public void ReportSingleTypeError(CodeRegion where, Type type) {
			Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: The expression has types {5}, but it must only have one.", where.FileName, where.StartRow, where.StartColumn, where.EndRow, where.EndColumn, type);
		}
	}
}
