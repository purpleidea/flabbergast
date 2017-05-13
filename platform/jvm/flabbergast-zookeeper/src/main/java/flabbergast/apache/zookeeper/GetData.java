package flabbergast.apache.zookeeper;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.Any;
import flabbergast.lang.AnyConverter;
import flabbergast.lang.Attribute;
import flabbergast.lang.AttributeSource;
import flabbergast.lang.Context;
import flabbergast.lang.Definition;
import flabbergast.lang.Frame;
import flabbergast.lang.Future;
import flabbergast.lang.SourceReference;
import flabbergast.lang.SpecialLocation;
import flabbergast.lang.Template;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

final class GetData implements LookupAssistant.Recipient {
  static final Definition DEFINITION =
      LookupAssistant.create(
          GetData::new,
          LookupAssistant.find(AnyConverter.asString(false), (i, x) -> i.path = x, "path"),
          LookupAssistant.find(
              AnyConverter.asTemplate(false), (i, x) -> i.template = x, "zookeeper", "node"),
          LookupAssistant.find(
              AnyConverter.asProxy(ZooKeeper.class, false, SpecialLocation.uri("zookeeper")),
              (i, x) -> i.owner = x,
              "zk_connection"));
  private static final Pattern SLASH = Pattern.compile("/");

  private ZooKeeper owner;
  private String path;
  private Template template;

  GetData() {}

  private String messageForError(int dataErrorCode) {
    return String.format(
        "ZooKeeper failure “%s” on “%s”.", KeeperException.Code.get(dataErrorCode).name(), path);
  }

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    owner.getData(
        path,
        false,
        (dataErrorCode, _a, _b, data, _c) -> {
          if (data == null) {
            if (dataErrorCode == KeeperException.Code.NONODE.intValue()) {
              future.complete(Any.NULL);
              return;
            }

            future.error(sourceReference, messageForError(dataErrorCode));
            return;
          }
          owner.getChildren(
              path,
              false,
              (childrenErrorCode, _y, _z, children) -> {
                if (children == null) {
                  future.error(sourceReference, messageForError(childrenErrorCode));
                  return;
                }

                final var pathParts = SLASH.split(path);
                future.complete(
                    Any.of(
                        Frame.create(
                            future,
                            sourceReference,
                            context,
                            template,
                            AttributeSource.of(
                                Attribute.of("path", Any.of(path)),
                                Attribute.of("data", Any.of(data)),
                                Attribute.of(
                                    "parent_path",
                                    pathParts.length > 1
                                        ? Any.of(
                                            Stream.of(pathParts)
                                                .limit(pathParts.length - 1)
                                                .collect(Collectors.joining("/", "/", "")))
                                        : Any.NULL),
                                Attribute.of(
                                    "child_paths",
                                    Any.of(
                                        Frame.create(
                                            future,
                                            sourceReference,
                                            context,
                                            AttributeSource.listOfAny(
                                                children
                                                    .stream()
                                                    .map(
                                                        child ->
                                                            String.format("%s/%s", path, child))
                                                    .map(Any::of)))))))));
              },
              null);
        },
        null);
  }
}
