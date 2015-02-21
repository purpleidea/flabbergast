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
}
