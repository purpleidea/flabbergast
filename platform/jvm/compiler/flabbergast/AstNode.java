package flabbergast;

/**
 * The base class for nodes in the syntax tree.
 *
 * This is present to hold all the fields needed to locate the syntax element
 * in the original file for generating debugging and error information.
 */
abstract class AstNode implements CodeRegion {
	private int start_row;
	private int start_column;
	private int end_row;
	private int end_column;
	private String file_name;

	public int getStartRow (){ return start_row; }
	public int getStartColumn (){ return start_column; }
	public int getEndRow (){ return end_row; }
	public int getEndColumn (){ return end_column; }
	public String getFileName (){ return file_name; }

	public abstract String getPrettyName ();
}