package flabbergast;

public interface ErrorCollector {
	void reportExpressionTypeError(CodeRegion where, TypeSet new_type, TypeSet existing_type);
	void reportLookupTypeError(CodeRegion where, String name, TypeSet new_type, TypeSet existing_type);
	void reportForbiddenNameAccess(CodeRegion where, String name);
	void reportParseError(String filename, int index, int row, int column, String message);
	void reportRawError(CodeRegion where, String message);
}
