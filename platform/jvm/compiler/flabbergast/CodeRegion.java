package flabbergast;

public interface CodeRegion {
    int getEndColumn();

    int getEndRow();

    String getFileName();

    String getPrettyName();

    int getStartColumn();

    int getStartRow();
}
