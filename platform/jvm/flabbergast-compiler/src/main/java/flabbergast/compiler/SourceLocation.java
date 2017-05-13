package flabbergast.compiler;

import java.util.Objects;
import org.objectweb.asm.commons.GeneratorAdapter;

/** The range of a source file matching some element or error in the file * */
public final class SourceLocation {
  public static final SourceLocation EMPTY = new SourceLocation("<unknown>", 1, 1, 1, 1);

  private final int endColumn;

  private final int endLine;

  private final String fileName;
  private final int startColumn;
  private final int startLine;

  public SourceLocation(
      String fileName, int startLine, int startColumn, int endLine, int endColumn) {
    super();
    this.fileName = fileName;
    this.startLine = startLine;
    this.startColumn = startColumn;
    this.endLine = endLine;
    this.endColumn = endColumn;
  }
  /** Create a new source location that starts at the end of this one. */
  public SourceLocation after() {
    return new SourceLocation(fileName, endLine, endColumn, endLine, endColumn);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final var that = (SourceLocation) o;
    return endColumn == that.endColumn
        && endLine == that.endLine
        && startColumn == that.startColumn
        && startLine == that.startLine
        && Objects.equals(fileName, that.fileName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endColumn, endLine, fileName, startColumn, startLine);
  }

  public int getEndColumn() {
    return endColumn;
  }

  public int getEndLine() {
    return endLine;
  }

  public String getFileName() {
    return fileName;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public int getStartLine() {
    return startLine;
  }

  /** Determines if this region ends after the provided location. */
  public boolean isFurther(SourceLocation other) {
    var compare = endLine - other.endLine;
    if (compare == 0) {
      compare = endColumn - other.endColumn;
    }
    return compare > 0;
  }

  /** Create a new location starting at the same position, but ending more columns further. */
  public SourceLocation plusColumns(int columns) {
    return new SourceLocation(fileName, startLine, startColumn, endLine, endColumn + columns);
  }

  /** Create a new location starting at the same position, but ending more lines later. */
  public SourceLocation plusLines(int lines) {
    return new SourceLocation(fileName, startLine, startColumn, endLine + lines, 0);
  }

  /** Write the location to the operand stack of a method as constants. */
  public void pushToStack(GeneratorAdapter methodGen) {
    methodGen.push(fileName);
    methodGen.push(startLine);
    methodGen.push(startColumn);
    methodGen.push(endLine);
    methodGen.push(endColumn);
  }

  public boolean sameStart(SourceLocation other) {
    return fileName.equals(other.fileName)
        && startLine == other.startLine
        && endColumn == other.endColumn;
  }

  /** Render a human-readable version of this location. */
  @Override
  public String toString() {
    return String.format(
        "%s:%d:%d-%d:%d", fileName, startLine + 1, startColumn + 1, endLine + 1, endColumn + 1);
  }
}
