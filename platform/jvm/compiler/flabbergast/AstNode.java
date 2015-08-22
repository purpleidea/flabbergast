package flabbergast;

import java.lang.Iterable;
import java.lang.StringBuilder;

/**
 * The base class for nodes in the syntax tree.
 * 
 * This is present to hold all the fields needed to locate the syntax element in
 * the original file for generating debugging and error information.
 */
abstract class AstNode implements CodeRegion {

	public static String join(String delimiter, Iterable<? extends Object> items) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Object item : items) {
			if (first) {
				first = false;
			} else {
				builder.append(delimiter);
			}
			builder.append(item.toString());
		}
		return builder.toString();
	}
	protected int end_column;
	protected int end_row;
	protected String file_name;
	protected int start_column;
	protected int start_row;

	public abstract void generateApi(ApiGenerator apigen, boolean collect_names);

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
