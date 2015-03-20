package flabbergast;

public interface CodeRegion {
	String getPrettyName ();
	int getStartRow ();
	int getStartColumn ();
	int getEndRow ();
	int getEndColumn ();
	String getFileName ();
}
