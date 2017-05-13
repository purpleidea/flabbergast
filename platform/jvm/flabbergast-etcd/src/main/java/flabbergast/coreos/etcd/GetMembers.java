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
import flabbergast.lang.SourceReference;
import flabbergast.lang.Template;
import io.etcd.jetcd.Client;

final class GetMembers implements LookupAssistant.Recipient {
  static final Definition DEFINITION =
      LookupAssistant.create(
          GetMembers::new,
          LookupAssistant.find(
              EtcdUriService.ETCD_CONNECTION_ANY_CONVERTER,
              (f, c) -> f.client = c,
              "etcd_connection"),
          LookupAssistant.find(
              AnyConverter.asTemplate(false), (f, v) -> f.template = v, "etcd", "member_tmpl"));

  private Client client;
  private Template template;

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    client
        .getClusterClient()
        .listMember()
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
                              .getMembers()
                              .stream()
                              .map(
                                  member ->
                                      Frame.define(
                                          template,
                                          AttributeSource.of(
                                              Attribute.of("member_id", Any.of(member.getId())),
                                              Attribute.of(
                                                  "name", Any.of(member.getName().getBytes())),
                                              Attribute.of(
                                                  "peer_uris",
                                                  Frame.define(
                                                      AttributeSource.listOfAny(
                                                          member
                                                              .getPeerURIs()
                                                              .stream()
                                                              .map(u -> Any.of(u.toString()))))),
                                              Attribute.of(
                                                  "client_uris",
                                                  Frame.define(
                                                      AttributeSource.listOfAny(
                                                          member
                                                              .getClientURIs()
                                                              .stream()
                                                              .map(
                                                                  u ->
                                                                      Any.of(u.toString()))))))))));
              future.complete(Any.of(result));
            });
  }
}
