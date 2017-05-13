package flabbergast.coreos.etcd;

import flabbergast.export.LookupAssistant;
import flabbergast.lang.Any;
import flabbergast.lang.AnyConverter;
import flabbergast.lang.Attribute;
import flabbergast.lang.AttributeSource;
import flabbergast.lang.Context;
import flabbergast.lang.Definition;
import flabbergast.lang.Frame;
import flabbergast.lang.Future;
import flabbergast.lang.Name;
import flabbergast.lang.SourceReference;
import flabbergast.lang.SpecialLocation;
import flabbergast.lang.Template;
import flabbergast.util.ConcurrentMapper;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.options.GetOption;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

final class GetKV implements LookupAssistant.Recipient {
  static final Definition DEFINITION =
      LookupAssistant.create(
          GetKV::new,
          LookupAssistant.find(
              EtcdUriService.ETCD_CONNECTION_ANY_CONVERTER,
              (f, c) -> f.client = c,
              "etcd_connection"),
          LookupAssistant.find(
              AnyConverter.frameOf(EtcdUriService.BYTE_ANY_CONVERTER, false),
              (f1, v1) -> f1.keys = v1,
              "args"),
          LookupAssistant.find(
              AnyConverter.asProxy(
                  GetOption.SortTarget.class,
                  true,
                  SpecialLocation.library("coreos", "etcd").attributes("field").any()),
              (f, v) -> f.target = v,
              "sort_by"),
          LookupAssistant.find(
              AnyConverter.asProxy(
                  GetOption.SortOrder.class,
                  true,
                  SpecialLocation.library("coreos", "etcd").attributes("sort").any()),
              (f, v) -> f.order = v,
              "order"),
          LookupAssistant.find(AnyConverter.asInt(true), (f, v) -> f.limit = v, "limit"),
          LookupAssistant.find(AnyConverter.asInt(true), (f, v) -> f.revision = v, "revision"),
          LookupAssistant.find(
              EtcdUriService.BYTE_ANY_CONVERTER_OR_NULL, (f, v) -> f.range = v, "stop_at"),
          LookupAssistant.find(AnyConverter.asBool(false), (f, v) -> f.isPrefix = v, "is_prefix"),
          LookupAssistant.find(
              AnyConverter.asTemplate(false), (f, v) -> f.template = v, "etcd", "key_value_tmpl"));

  private Client client;
  private boolean isPrefix;
  private Map<Name, ByteSequence> keys;
  private Long limit;
  private GetOption.SortOrder order;
  private ByteSequence range;
  private Long revision;
  private GetOption.SortTarget target;
  private Template template;

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    if (keys.size() == 0) {
      future.complete(Any.of(Frame.create(future, sourceReference, context)));
      return;
    }

    ConcurrentMapper.process(
        keys.entrySet(),
        new ConcurrentMapper<Entry<Name, ByteSequence>, Attribute>() {
          @Override
          public void emit(List<Attribute> results) {
            future.complete(
                Any.of(
                    Frame.create(future, sourceReference, context, AttributeSource.of(results))));
          }

          @Override
          public void process(
              Entry<Name, ByteSequence> entry, int index, Consumer<Attribute> output) {
            final var builder = GetOption.newBuilder();
            if (order != null) {
              builder.withSortOrder(order);
            }
            if (target != null) {
              builder.withSortField(target);
            }
            if (limit != null) {
              builder.withLimit(limit);
            }
            if (revision != null) {
              builder.withRevision(revision);
            }
            if (range != null) {
              builder.withRange(range);
            }
            if (isPrefix) {
              builder.withPrefix(entry.getValue());
            }
            client
                .getKVClient()
                .get(isPrefix ? null : entry.getValue(), builder.build())
                .whenComplete(
                    (response, err) -> {
                      if (err != null) {
                        future.error(sourceReference, err.getMessage());
                        return;
                      }
                      final var result =
                          Frame.create(
                              future,
                              sourceReference,
                              context,
                              AttributeSource.listOfDefinition(
                                  response
                                      .getKvs()
                                      .stream()
                                      .map(
                                          value ->
                                              Frame.define(
                                                  template,
                                                  AttributeSource.of(
                                                      Attribute.of(
                                                          "key", Any.of(value.getKey().getBytes())),
                                                      Attribute.of(
                                                          "value",
                                                          Any.of(value.getValue().getBytes())),
                                                      Attribute.of(
                                                          "created",
                                                          Any.of(value.getCreateRevision())),
                                                      Attribute.of(
                                                          "modified",
                                                          Any.of(value.getModRevision())),
                                                      Attribute.of(
                                                          "version",
                                                          Any.of(value.getVersion())))))));
                      output.accept(Attribute.of(entry.getKey(), Any.of(result)));
                    });
          }
        });
  }
}
