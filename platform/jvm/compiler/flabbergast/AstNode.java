package flabbergast;

/**
 * The base class for nodes in the syntax tree.
 * 
 * This is present to hold all the fields needed to locate the syntax element in
 * the original file for generating debugging and error information.
 */
abstract class AstNode implements CodeRegion {
	private int end_column;
	private int end_row;
	private String file_name;
	private int start_column;
	private int start_row;

	@Override
	public int getEndColumn() {
		return end_column;
	}

	@Override
	public int getEndRow() {
		return end_row;
	}

	@Override
	public String getFileName() {
		return file_name;
	}

	@Override
	public abstract String getPrettyName();

	@Override
	public int getStartColumn() {
		return start_column;
	}

	@Override
	public int getStartRow() {
		return start_row;
	}
}