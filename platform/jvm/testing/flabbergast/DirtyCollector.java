package flabbergast;

public class DirtyCollector implements ErrorCollector {
	private boolean analyse_dirty;
	private boolean parse_dirty;

	public boolean isAnalyseDirty() {
		return analyse_dirty;
	}

	public boolean isParseDirty() {
		return parse_dirty;
	}

	@Override
	public void reportExpressionTypeError(CodeRegion where, TypeSet new_type,
			TypeSet existing_type) {
		analyse_dirty = true;

	}

	@Override
	public void reportForbiddenNameAccess(CodeRegion environment, String name) {
		analyse_dirty = true;
	}

	@Override
	public void reportLookupTypeError(CodeRegion where, String name,
			TypeSet new_type, TypeSet existing_type) {
		analyse_dirty = true;

	}

	@Override
	public void reportParseError(String filename, int index, int row,
			int column, String message) {
		parse_dirty = true;
	}

	@Override
	public void reportRawError(CodeRegion where, String message) {
		analyse_dirty = true;
	}

	@Override
	public void reportSingleTypeError(CodeRegion where, TypeSet type) {
		analyse_dirty = true;
	}
}
