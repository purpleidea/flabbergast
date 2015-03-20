package flabbergast;

public class ConsoleCollector implements ErrorCollector {
	public void reportExpressionTypeError(CodeRegion where, TypeSet new_type,
			TypeSet existing_type) {
		if (existing_type.isEmpty()) {
			System.err
					.printf("%s:%d:%d-%d:%d: No possible type for %s. Expression should have types: %s.\n",
							where.getFileName(), where.getStartRow(),
							where.getStartColumn(), where.getEndRow(),
							where.getEndColumn(), where.getPrettyName(),
							new_type);
		} else {
			System.err
					.printf("%s:%d:%d-%d:%d: Conflicting types for %s: %s versus %s.\n",
							where.getFileName(), where.getStartRow(),
							where.getStartColumn(), where.getEndRow(),
							where.getEndColumn(), where.getPrettyName(),
							new_type,
							existing_type);
		}
	}

	public void reportLookupTypeError(CodeRegion environment, String name,
			TypeSet new_type, TypeSet existing_type) {
		if (existing_type.isEmpty()) {
			System.err
					.printf("%s:%d:%d-%d:%d: No possible type for “%s”. Expression should have types: %s.\n",
							environment.getFileName(),
							environment.getStartRow(),
							environment.getStartColumn(),
							environment.getEndRow(),
							environment.getEndColumn(), name,
							new_type);
		} else {
			System.err
					.printf("%s:%d:%d-%d:%d: Lookup for “%s” has conflicting types: %s versus %s.\n",
							environment.getFileName(),
							environment.getStartRow(),
							environment.getStartColumn(),
							environment.getEndRow(),
							environment.getEndColumn(), name,
							new_type,
							existing_type);
		}
	}

	public void reportForbiddenNameAccess(CodeRegion environment, String name) {
		System.err.printf("%s:%d:%d-%d:%d: Lookup for “%s” is forbidden.\n",
				environment.getFileName(), environment.getStartRow(),
				environment.getStartColumn(), environment.getEndRow(),
				environment.getEndColumn(), name);
	}

	public void reportParseError(String filename, int index, int row,
			int column, String message) {
		System.err.printf("%s:%d:%d: %s\n", filename, row, column, message);
	}

	public void reportRawError(CodeRegion where, String message) {
		System.err.printf("%s:%d:%d-%d:%d: %s\n", where.getFileName(),
				where.getStartRow(), where.getStartColumn(), where.getEndRow(),
				where.getEndColumn(), message);
	}
}
