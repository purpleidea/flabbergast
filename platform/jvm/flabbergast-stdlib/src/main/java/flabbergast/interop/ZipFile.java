package flabbergast.interop;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.nio.charset.StandardCharsets;

final class ZipFile extends BaseZipFile {
  static final Definition DEFINITION =
      make(
          ZipFile::new,
          LookupAssistant.find(
              AnyConverter.asBinOrStr(false, StandardCharsets.UTF_8),
              (z, f) -> z.contents = f,
              "contents"));
  private static final byte[] NULL = new byte[0];
  private byte[] contents;

  @Override
  protected byte[] contents() {
    return contents;
  }

  @Override
  protected byte[] linkTarget() {
    return NULL;
  }
}
