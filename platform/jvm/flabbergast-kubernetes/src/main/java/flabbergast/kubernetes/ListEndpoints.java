package flabbergast.kubernetes;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.lang.*;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Operation;
import java.util.List;

final class ListEndpoints extends BaseKubernetesNamespacedListOperation<Endpoints, EndpointsList> {

  @Override
  protected Operation<Endpoints, EndpointsList, ?, ?> invokeNamespace(KubernetesClient client)
      throws KubernetesClientException {
    return client.endpoints();
  }

  @Override
  protected List<Endpoints> items(EndpointsList list) {
    return list.getItems();
  }

  @Override
  protected void populate(List<Attribute> builder, Endpoints item) {
    builder.add(Attribute.of("name", Any.of(item.getMetadata().getName())));
    builder.add(Attribute.of("namespace", Any.of(item.getMetadata().getNamespace())));
    builder.add(
        Attribute.of(
            "subsets",
            Frame.define(
                AttributeSource.list(
                    Attribute::of,
                    item.getSubsets()
                        .stream()
                        .map(
                            subset ->
                                Template.instantiate(
                                    AttributeSource.of(
                                        Attribute.of(
                                            "ports",
                                            Frame.define(
                                                subset
                                                    .getPorts()
                                                    .stream()
                                                    .map(
                                                        port ->
                                                            Attribute.of(
                                                                port.getName(),
                                                                Template.instantiate(
                                                                    AttributeSource.of(
                                                                        Attribute.of(
                                                                            "port",
                                                                            Any.of(port.getPort())),
                                                                        Attribute.of(
                                                                            "protocol",
                                                                            Any.of(
                                                                                port
                                                                                    .getProtocol()))),
                                                                    "Kubernetes endpoint port",
                                                                    "kubernetes",
                                                                    "subset_port_tmpl")))
                                                    .collect(toSource()))),
                                        Attribute.of(
                                            "addresses",
                                            Frame.define(
                                                AttributeSource.list(
                                                    Attribute::of,
                                                    subset
                                                        .getAddresses()
                                                        .stream()
                                                        .map(
                                                            address ->
                                                                Template.instantiate(
                                                                    AttributeSource.of(
                                                                        Attribute.of(
                                                                            "ip",
                                                                            Any.of(
                                                                                address.getIp())),
                                                                        Attribute.of(
                                                                            "nodename",
                                                                            Any.of(
                                                                                address
                                                                                    .getNodeName())),
                                                                        Attribute.of(
                                                                            "hostname",
                                                                            Any.of(
                                                                                address
                                                                                    .getHostname()))),
                                                                    "Kubernetes endpoint address",
                                                                    "kubernetes",
                                                                    "subset_address_tmpl")))))),
                                    "Kubernetes endpoint subset",
                                    "kubernetes",
                                    "subset_tmpl"))))));
  }
}
