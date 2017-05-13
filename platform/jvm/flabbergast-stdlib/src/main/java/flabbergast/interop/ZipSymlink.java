package flabbergast.interop;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.AnyConverter;
import flabbergast.lang.Definition;
import java.nio.charset.StandardCharsets;

final class ZipSymlink extends BaseZipFile {
  static final Definition DEFINITION =
      make(
          ZipSymlink::new,
          LookupAssistant.find(AnyConverter.asString(false), (h, v) -> h.target = v));
  private String target;

  @Override
  protected byte[] contents() {
    return null;
  }

  @Override
  protected byte[] linkTarget() {
    return target.getBytes(StandardCharsets.UTF_8);
  }
}
