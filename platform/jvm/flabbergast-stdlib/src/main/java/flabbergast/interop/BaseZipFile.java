package flabbergast.interop;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

abstract class BaseZipFile implements LookupAssistant.Recipient {

  @SafeVarargs
  static <T extends BaseZipFile> Definition make(Supplier<T> ctor, LookupAssistant<T>... savers) {
    return LookupAssistant.create(
        ctor,
        Stream.concat(
            Stream.of(savers),
            Stream.of(
                LookupAssistant.find(
                    AnyConverter.asString(false), (z, n) -> z.fileName = n, "file_name"),
                LookupAssistant.find(
                    AnyConverter.asDateTime(false), (z, n) -> z.mtime = n, "mtime"),
                LookupAssistant.find(AnyConverter.asInt(false), (z, n) -> z.user = n, "user"),
                LookupAssistant.find(AnyConverter.asInt(false), (z, n) -> z.group = n, "group"),
                LookupAssistant.find(AnyConverter.asInt(false), (z, n) -> z.others = n, "other"),
                LookupAssistant.find(AnyConverter.asInt(false), (z, n) -> z.uid = n, "uid"),
                LookupAssistant.find(AnyConverter.asInt(false), (z, n) -> z.gid = n, "gid"))));
  }

  String fileName;
  long gid;
  long group;
  ZonedDateTime mtime;
  long others;
  long uid;
  long user;

  protected abstract byte[] contents();

  protected abstract byte[] linkTarget();

  @Override
  public final void run(Future<Any> future, SourceReference sourceReference, Context context) {
    future.complete(Any.of(Frame.proxyOf(sourceReference, context, this, Stream.empty())));
  }

  final void write(ZipOutputStream stream) throws IOException {
    final var entry = new ZipEntry(fileName);
    entry.setLastModifiedTime(FileTime.from(mtime.toInstant()));
    final var linkTarget = linkTarget();
    final var extra = ByteBuffer.allocate(18 + linkTarget.length).order(ByteOrder.LITTLE_ENDIAN);
    extra.putShort((short) 0x756E);
    extra.putShort((short) extra.capacity());
    extra.putInt(0);
    extra.putShort((short) ((user & 7) << 6 | (group & 7) << 3 | (others & 7)));
    extra.putInt(linkTarget.length);
    extra.putShort((short) uid);
    extra.putShort((short) gid);
    extra.put(linkTarget);
    final var crc = new CRC32();
    crc.update(extra.position(4));
    extra.putInt(4, (int) crc.getValue());
    entry.setExtra(extra.array());
    stream.putNextEntry(entry);
    final var contents = contents();
    if (contents != null) {
      stream.write(contents);
    }
  }
}
